CREATE INDEX IF NOT EXISTS idx_recipe_moderation_actions_recipe_id_created_at
    ON recipe_moderation_actions (recipe_id, created_at DESC);