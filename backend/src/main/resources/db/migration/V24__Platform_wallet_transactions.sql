-- ============================================================================
-- V24 - Historique des mouvements des wallets plateforme
-- ============================================================================

CREATE TABLE IF NOT EXISTS platform_wallet_transactions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_type  TEXT        NOT NULL,
    amount_cents BIGINT      NOT NULL,
    reason       TEXT        NOT NULL,
    reference_id UUID,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
