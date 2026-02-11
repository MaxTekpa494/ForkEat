-- Extension pour full-text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Index GIN pour recherche full-text sur title + summary
CREATE INDEX IF NOT EXISTS idx_recipe_title_summary_fts
    ON recipes USING GIN (to_tsvector('french', title || ' ' || COALESCE(summary, '')));

-- Index GIN pour recherche full-text sur les ingrédients
CREATE INDEX IF NOT EXISTS idx_ingredient_name_fts
    ON ingredients USING GIN (to_tsvector('french', name));
