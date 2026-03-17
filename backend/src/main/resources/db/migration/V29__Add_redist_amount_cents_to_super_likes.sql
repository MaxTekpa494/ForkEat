-- Ajout de la part redistribution figée à la création du SuperLike.
-- Calculée une seule fois (prix × (1 - earningsRatio)) et immutable après insertion.
-- Le batch mensuel lit cette valeur directement depuis Neo4j (répliquée via Debezium).
ALTER TABLE super_likes
    ADD COLUMN redist_amount_cents BIGINT NOT NULL DEFAULT 0;