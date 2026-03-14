package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.SuperLikeConfigEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.SuperLikeConfigRepository;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;
import fr.uge.forkeat.service.persistence.SuperLikeConfigPersistence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SuperLikeConfigPersistenceAdapter implements SuperLikeConfigPersistence {

    private final SuperLikeConfigRepository superLikeConfigRepository;

    public SuperLikeConfigPersistenceAdapter(SuperLikeConfigRepository superLikeConfigRepository) {
        this.superLikeConfigRepository = superLikeConfigRepository;
    }

    @Override
    public SuperLikeConfig get() {
        return superLikeConfigRepository.findAll().stream()
                .findFirst()
                .map(SuperLikeConfigEntityMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("super_like_config table is empty — check migration V22"));
    }

    @Override
    public SuperLikeConfig update(long priceCents, BigDecimal earningsRatio) {
        var entity = superLikeConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("super_like_config table is empty — check migration V22"));
        entity.setPriceCents(priceCents);
        entity.setEarningsRatio(earningsRatio);
        return SuperLikeConfigEntityMapper.toDomain(superLikeConfigRepository.save(entity));
    }
}
