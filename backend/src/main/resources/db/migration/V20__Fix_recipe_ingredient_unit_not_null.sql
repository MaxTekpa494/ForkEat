-- V20 : Normalisation de la colonne unit dans recipe_ingredients
-- "Pas d'unité" est représenté par '' (chaîne vide) et non NULL.
-- On corrige les lignes existantes puis on ajoute la contrainte NOT NULL.

UPDATE "recipe_ingredients"
SET "unit" = ''
WHERE "unit" IS NULL;

ALTER TABLE "recipe_ingredients"
    ALTER COLUMN "unit" SET NOT NULL,
    ALTER COLUMN "unit" SET DEFAULT '';
