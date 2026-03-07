-- ============================================================================
-- V22 - Système de promotions et configuration dynamique des super-likes
-- ============================================================================

-- 1. Configuration de base du super-like (singleton : une seule ligne)
CREATE TABLE IF NOT EXISTS super_like_config (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    price_cents    BIGINT       NOT NULL CHECK (price_cents > 0),
    earnings_ratio NUMERIC(5,4) NOT NULL CHECK (earnings_ratio > 0 AND earnings_ratio < 1),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO super_like_config (price_cents, earnings_ratio)
SELECT 100, 0.4000
WHERE NOT EXISTS (SELECT 1 FROM super_like_config);

-- 2. Type enum pour le statut des promotions (idempotent : peut déjà exister)
DO $$ BEGIN
    CREATE TYPE promotion_status AS ENUM ('SCHEDULED', 'ACTIVE', 'EXPIRED', 'CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 3. Table des promotions planifiables
-- Si l'ancienne table promotions existe sans notre colonne bonus_every_n,
-- on la supprime pour créer la nouvelle structure.
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'promotions'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'promotions' AND column_name = 'bonus_every_n'
    ) THEN
        DROP TABLE promotions CASCADE;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS promotions (
    id            UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255)     NOT NULL,
    starts_at     TIMESTAMPTZ      NOT NULL,
    ends_at       TIMESTAMPTZ,                  -- NULL = sans date de fin explicite
    price_cents   BIGINT           NOT NULL CHECK (price_cents > 0),
    bonus_every_n INT              CHECK (bonus_every_n IS NULL OR bonus_every_n > 0),
    status        promotion_status NOT NULL DEFAULT 'SCHEDULED',
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_promo_dates CHECK (ends_at IS NULL OR starts_at < ends_at)
);

-- 4. Enrichissement de super_likes pour l'audit financier et le reporting
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'super_likes' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE super_likes ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'super_likes' AND column_name = 'promotion_id'
    ) THEN
        ALTER TABLE super_likes ADD COLUMN promotion_id UUID REFERENCES promotions(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'super_likes' AND column_name = 'is_bonus_free'
    ) THEN
        ALTER TABLE super_likes ADD COLUMN is_bonus_free BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

-- Nécessaire pour que Debezium CDC capte les nouvelles colonnes
ALTER TABLE super_likes REPLICA IDENTITY FULL;
