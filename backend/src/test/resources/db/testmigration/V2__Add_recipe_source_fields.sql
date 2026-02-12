-- ============================================
-- Ajout des champs de provenance pour les recettes importées
-- ============================================

ALTER TABLE "recipe" ADD COLUMN "source" VARCHAR(50);

ALTER TABLE "recipe" ADD COLUMN "external_id" VARCHAR(100);

CREATE INDEX "idx_recipe_source" ON "recipe" ("source", "external_id");

-- Contrainte d'unicité partielle : une recette importée (avec source et external_id) ne peut pas être dupliquée
CREATE UNIQUE INDEX "idx_recipe_source_external_id_unique" ON "recipe" ("source", "external_id")
    WHERE "source" IS NOT NULL AND "external_id" IS NOT NULL;
