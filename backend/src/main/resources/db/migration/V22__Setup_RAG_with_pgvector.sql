
CREATE EXTENSION IF NOT EXISTS vector;


-- Table denormalisée pour le RAG (clone de la table recipes)
CREATE TABLE recipe_rag (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id        UUID NOT NULL UNIQUE REFERENCES recipes(id) ON DELETE CASCADE,
    document_text    TEXT NOT NULL,
    embedding        vector(1024),
    title            TEXT NOT NULL,
    ingredients      TEXT,
    dietaries        TEXT,
    allergens        TEXT,
    ready_in_minutes INT,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_recipe_rag_embedding
    ON recipe_rag USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);


CREATE INDEX idx_recipe_rag_recipe_id     ON recipe_rag(recipe_id);
CREATE INDEX idx_recipe_rag_ready_minutes ON recipe_rag(ready_in_minutes);


CREATE INDEX idx_recipe_rag_ingredients_trgm
    ON recipe_rag USING GIN (ingredients gin_trgm_ops);
CREATE INDEX idx_recipe_rag_dietaries_trgm
    ON recipe_rag USING GIN (dietaries gin_trgm_ops);
CREATE INDEX idx_recipe_rag_allergens_trgm
    ON recipe_rag USING GIN (allergens gin_trgm_ops);

