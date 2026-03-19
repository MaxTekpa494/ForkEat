-- Ajout de la valeur DISMISSED à l'énumération recipe_moderation_action_type
ALTER TYPE "user_moderation_action_type" ADD VALUE IF NOT EXISTS 'DISMISSED';

