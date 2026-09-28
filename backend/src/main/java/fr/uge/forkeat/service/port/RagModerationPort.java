package fr.uge.forkeat.service.port;

import java.util.Optional;

public interface RagModerationPort {

  Optional<String> moderate(String input);
}
