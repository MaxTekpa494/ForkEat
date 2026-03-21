CREATE INDEX IF NOT EXISTS idx_user_moderation_actions_user_id_created_at
    ON user_moderation_actions (user_id, created_at DESC);

-- Ajout de la valeur DISMISSED à l'énumération recipe_moderation_action_type
ALTER TYPE "user_moderation_action_type" ADD VALUE IF NOT EXISTS 'DISMISSED';

