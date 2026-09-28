package fr.uge.forkeat.infrastructure.ai.adapter;

import fr.uge.forkeat.service.port.RagModerationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

@Component
public final class RagModerationAdapter implements RagModerationPort {

  private static final Logger log = LoggerFactory.getLogger(RagModerationAdapter.class);

  @Value("${app.rag.moderation.min-length:3}")
  private int minLength;

  @Value("${app.rag.moderation.max-length:500}")
  private int maxLength;

  @Value("${app.rag.moderation.llm-timeout-seconds:15}")
  private int llmTimeoutSeconds;

  // Tentatives de prompt injection
  private static final Pattern PROMPT_INJECTION = Pattern.compile(
          "(?i)(ignore|forget|bypass|override).{0,30}(instruction|rule|system|prompt)" +
                  "|(system prompt|previous instruction|jailbreak|act as|you are now)",
          Pattern.CASE_INSENSITIVE
  );

  // Tentatives d'extraction de données
  private static final Pattern DATA_EXFILTRATION = Pattern.compile(
          "(?i)(email|password|mot de passe|token|secret|api.?key|" +
                  "wallet|solde|carte.?bancaire|iban|credit.?card)",
          Pattern.CASE_INSENSITIVE
  );

  // Tentatives d'injection SQL
  private static final Pattern SQL_INJECTION = Pattern.compile(
          "(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|UNION)" +
                  ".{0,20}(FROM|INTO|TABLE|WHERE|DATABASE)",
          Pattern.CASE_INSENSITIVE
  );

  private final ChatModel chatModel;

  public RagModerationAdapter(ChatModel chatModel) {
    this.chatModel = chatModel;
  }

  @Override
  public Optional<String> moderate(String input) {
    var javaResult = applyJavaRules(input);
    if (javaResult.isPresent()) {
      return javaResult;
    }
    return applyLlmModeration(input);
  }

  private Optional<String> applyJavaRules(String input) {
    if (input == null || input.isBlank()) {
      return Optional.of("La requête ne peut pas être vide.");
    }
    if (input.length() < minLength) {
      return Optional.of("La requête est trop courte (minimum " + minLength + " caractères).");
    }
    if (input.length() > maxLength) {
      return Optional.of("La requête est trop longue (maximum " + maxLength + " caractères).");
    }
    if (SQL_INJECTION.matcher(input).find()) {
      return Optional.of("Contenu non autorisé détecté (SQL).");
    }
    if (PROMPT_INJECTION.matcher(input).find()) {
      return Optional.of("Tentative de manipulation du système détectée.");
    }
    if (DATA_EXFILTRATION.matcher(input).find()) {
      return Optional.of("Requête non autorisée (données sensibles).");
    }
    return Optional.empty();
  }

  private Optional<String> applyLlmModeration(String input) {
    try {
      var message = (Message) new UserMessage(input);
      var future = CompletableFuture.supplyAsync(() -> chatModel.call(new Prompt(message)));
      var response = future.get(llmTimeoutSeconds, TimeUnit.SECONDS);

      var result = response.getResult().getOutput().getText().trim().toLowerCase();
      log.debug("[MODERATION] llama-guard3 response: '{}'", result);

      if (!result.startsWith("safe")) {
        var category = result.contains("\n")
                ? result.substring(result.indexOf("\n") + 1).trim()
                : "non spécifiée";
        return Optional.of("Contenu inapproprié détecté (catégorie : " + category + ").");
      }
    } catch (TimeoutException e) {
      log.warn("[MODERATION] llama-guard3 timeout ({}s), modération LLM ignorée", llmTimeoutSeconds);
    } catch (Exception e) { // fail-open
      log.warn("[MODERATION] llama-guard3 indisponible, modération LLM ignorée : {}", e.getMessage());
    }
    return Optional.empty();
  }
}
