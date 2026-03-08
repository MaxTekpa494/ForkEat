-- V18 : Suppression des colonnes source et external_id devenues inutiles
-- Ces champs servaient à l'import depuis des APIs externes (Spoonacular)
-- Les recettes importées sont maintenant gérées directement via Flyway
-- On en a plus besion car la strate du RAG a changé

DROP INDEX IF EXISTS "idx_recipe_source";
DROP INDEX IF EXISTS "idx_recipe_source_external_id_unique";

ALTER TABLE "recipes" DROP COLUMN IF EXISTS "source";
ALTER TABLE "recipes" DROP COLUMN IF EXISTS "external_id";
