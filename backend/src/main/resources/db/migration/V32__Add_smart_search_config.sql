CREATE TABLE IF NOT EXISTS smart_search_config (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    top_k       INT         NOT NULL CHECK (top_k > 0),
    cost        BIGINT      NOT NULL CHECK (cost > 0),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO smart_search_config (top_k, cost)
SELECT 10, 10
WHERE NOT EXISTS (SELECT 1 FROM smart_search_config);
