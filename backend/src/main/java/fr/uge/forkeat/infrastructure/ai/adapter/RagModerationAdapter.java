package fr.uge.forkeat.infrastructure.ai.adapter;

import fr.uge.forkeat.service.exception.ModerationRagException;
import fr.uge.forkeat.service.port.RagModerationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Component
public final class RagModerationAdapter implements RagModerationPort {

  private static final Logger log = LoggerFactory.getLogger(RagModerationAdapter.class);

  @Value("${app.rag.moderation.min-length:3}")
  private int minLength;

  @Value("${app.rag.moderation.max-length:500}")
  private int maxLength;


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

  private final Consumer<String> MODERATEUR = (input) -> {
    applyJavaRules(input);
    applyLlmModeration(input);
  };

  public RagModerationAdapter(ChatModel chatModel) {
    this.chatModel = chatModel;
  }





  @Override
  public void assertSafe(String input) {
//    // Ici ça nous coute rien
//    applyJavaRules(input);
//
//    // Par contre ici, faudrait voir si on peut integrer un timeout...
//    applyLlmModeration(input);
    MODERATEUR.accept(input);
  }

  private void applyJavaRules(String input) {
    if (input == null || input.isBlank()) {
      throw new ModerationRagException("La requête ne peut pas être vide.");
    }
    if (input.length() < minLength) {
      throw new ModerationRagException("La requête est trop courte (minimum " + minLength + " caractères).");
    }
    if (input.length() > maxLength) {
      throw new ModerationRagException("La requête est trop longue (maximum " + maxLength + " caractères).");
    }
    if (SQL_INJECTION.matcher(input).find()) {
      throw new ModerationRagException("Contenu non autorisé détecté (SQL).");
    }
    if (PROMPT_INJECTION.matcher(input).find()) {
      throw new ModerationRagException("Tentative de manipulation du système détectée.");
    }
    if (DATA_EXFILTRATION.matcher(input).find()) {
      throw new ModerationRagException("Requête non autorisée (données sensibles).");
    }
  }

  private void applyLlmModeration(String input){
    try {
      var options = ChatOptions.builder().model("llama-guard3:1b").build();
      // UserMessage implements Message mais et Prompt prend Message en parametre
      // Je ne comprends donc pas pourquoi il n'accepte pas mon UserMessage
      // Par consequent je cast...
      var message = (Message)new UserMessage(input);
      var response = chatModel.call(new Prompt(message, options));
      var result = response.getResult().getOutput().getText().trim().toLowerCase();
      log.debug("[MODERATION] llama-guard3 response: '{}'", result);

      if (!result.startsWith("safe")) { // Il renvoie safe ou unsafe
        var category = result.contains("\n")
                ? result.substring(result.indexOf("\n") + 1).trim()
                : "non spécifiée";
        throw new ModerationRagException("Contenu inapproprié détecté (catégorie : " + category + ").");
      }
    }catch (ModerationRagException e){
      throw e;
    }catch (Exception e){
      log.warn("[MODERATION] llama-guard3 indisponible, modération LLM ignorée : {}", e.getMessage());
    }
  }
}
