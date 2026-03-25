-- Suppression en cascade des signalements lorsqu'une recette est supprimée.
-- Un signalement n'a aucune valeur sans la recette associée.
ALTER TABLE recipe_reports
    DROP CONSTRAINT recipe_report_recipe_id_fkey;

ALTER TABLE recipe_reports
    ADD CONSTRAINT recipe_report_recipe_id_fkey
        FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE;