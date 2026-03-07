package fr.uge.forkeat.service.port;

import java.util.List;
import java.util.UUID;

public interface RagSearchPort {

  List<UUID> findSimilarRecipeIds(String userQuery, int topK);

}
