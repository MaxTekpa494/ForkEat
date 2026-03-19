CREATE INDEX IF NOT EXISTS idx_user_moderation_actions_user_id_created_at
    ON user_moderation_actions (user_id, created_at DESC);