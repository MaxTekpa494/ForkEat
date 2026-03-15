package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;

import java.math.BigDecimal;

public interface SuperLikeConfigPersistence {
    SuperLikeConfig get();
    SuperLikeConfig update(long priceCents, BigDecimal earningsRatio);
}
