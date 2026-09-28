package fr.uge.forkeat.service;

import fr.uge.forkeat.service.model.smartsearch.SmartSearchConfig;
import fr.uge.forkeat.service.persistence.SmartSearchConfigPersistence;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmartSearchConfigService {

    private final SmartSearchConfigPersistence configPersistence;
    private final AuthenticationPort authPort;
    private final Logger logger = LoggerFactory.getLogger(SmartSearchConfigService.class);

    public SmartSearchConfigService(SmartSearchConfigPersistence configPersistence, AuthenticationPort authPort) {
        this.configPersistence = configPersistence;
        this.authPort = authPort;
    }

    public SmartSearchConfig getConfig() {
        return configPersistence.get();
    }

    @Transactional
    public SmartSearchConfig updateConfig(int topK, long cost) {
        if (!authPort.isAdmin()) {
            throw new AccessDeniedException("Only admins can update smart search config");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0");
        }
        if (cost <= 0) {
            throw new IllegalArgumentException("cost must be > 0");
        }
        var updated = configPersistence.update(topK, cost);
        logger.info("SmartSearch config updated: topK={}, cost={}", topK, cost);
        return updated;
    }
}
