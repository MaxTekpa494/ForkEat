-- V21 (test) : Table recipe_rag sans pgvector ni index HNSW/trigram
-- L'extension pgvector et la colonne embedding ne sont pas requises en test
-- (embedding est @Transient dans RecipeRagEntity — Hibernate ne la valide pas).

CREATE TABLE IF NOT EXISTS "recipe_rag" (
    "id"               UUID      NOT NULL,
    "recipe_id"        UUID      NOT NULL UNIQUE,
    "document_text"    TEXT      NOT NULL,
    "title"            TEXT      NOT NULL,
    "ingredients"      TEXT,
    "dietaries"        TEXT,
    "allergens"        TEXT,
    "ready_in_minutes" INTEGER,
    "created_at"       TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at"       TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY ("id"),
    FOREIGN KEY ("recipe_id") REFERENCES "recipes" ("id") ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS "idx_recipe_rag_recipe_id" ON "recipe_rag" ("recipe_id");
