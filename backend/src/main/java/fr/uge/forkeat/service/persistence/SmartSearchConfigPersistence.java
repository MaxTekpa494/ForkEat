package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.smartsearch.SmartSearchConfig;

public interface SmartSearchConfigPersistence {
    SmartSearchConfig get();
    SmartSearchConfig update(int topK, long cost);
}
