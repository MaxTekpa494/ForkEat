-- ============================================================================
-- V23 - Ajout des valeurs manquantes dans l'enum promotion_status
-- V1 avait créé promotion_status avec seulement ('SCHEDULED', 'ACTIVE', 'COMPLETED').
-- V22 n'a pas pu recréer le type (déjà existant). On ajoute les valeurs manquantes.
-- ============================================================================

ALTER TYPE promotion_status ADD VALUE IF NOT EXISTS 'EXPIRED';
ALTER TYPE promotion_status ADD VALUE IF NOT EXISTS 'CANCELLED';
