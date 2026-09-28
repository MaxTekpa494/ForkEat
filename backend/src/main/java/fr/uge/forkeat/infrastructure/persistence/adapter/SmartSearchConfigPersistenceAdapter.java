package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.SmartSearchConfigEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.SmartSearchConfigRepository;
import fr.uge.forkeat.service.model.smartsearch.SmartSearchConfig;
import fr.uge.forkeat.service.persistence.SmartSearchConfigPersistence;
import org.springframework.stereotype.Component;

@Component
public class SmartSearchConfigPersistenceAdapter implements SmartSearchConfigPersistence {

    private final SmartSearchConfigRepository repository;

    public SmartSearchConfigPersistenceAdapter(SmartSearchConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public SmartSearchConfig get() {
        return repository.findAll().stream()
                .findFirst()
                .map(SmartSearchConfigEntityMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("smart_search_config table is empty"));
    }

    @Override
    public SmartSearchConfig update(int topK, long cost) {
        var entity = repository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("smart_search_config table is empty"));
        entity.setTopK(topK);
        entity.setCost(cost);
        return SmartSearchConfigEntityMapper.toDomain(repository.save(entity));
    }
}
