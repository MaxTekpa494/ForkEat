/*
 * V5 - Configuration CDC, Initialisation des Wallets Système et Nettoyage des données de seed.
 *
 * Changements inclus :
 * 1. Activation de REPLICA IDENTITY FULL pour Debezium.
 * 2. Refonte de l'enum 'user_system_type'.
 * 3. Initialisation des utilisateurs système (Earnings & Redistribution) et de leurs wallets.
 * 4. Réassignation de toutes les recettes initiales (V3) à l'utilisateur 'system_earnings'.
 * 5. Suppression de l'ancien utilisateur d'import ('system_import') pour anonymiser les données.
 */

-- ============================================================================
-- 1. Configuration CDC (Change Data Capture)
-- ============================================================================
ALTER TABLE recipes REPLICA IDENTITY FULL;
ALTER TABLE "users" REPLICA IDENTITY FULL;

-- ============================================================================
-- 2. Mise à jour de l'Enum user_system_type
-- ============================================================================
ALTER TYPE user_system_type RENAME TO user_system_type_old;

CREATE TYPE user_system_type AS ENUM (
    'USER_WALLET_EARNINGS',
    'USER_WALLET_REDISTRIBUTION'
);

-- Nettoyage préventif
DELETE FROM user_systems;

ALTER TABLE user_systems
    ALTER COLUMN system_type TYPE user_system_type
    USING (system_type::text::user_system_type);

DROP TYPE user_system_type_old;

-- ============================================================================
-- 3. Initialisation des Données Système et Migration des Recettes
-- ============================================================================
DO $$
DECLARE
    earnings_user_id UUID := gen_random_uuid();
    redistribution_user_id UUID := gen_random_uuid();
    earnings_wallet_id UUID := gen_random_uuid();
    redistribution_wallet_id UUID := gen_random_uuid();
    old_user_id UUID := 'aa322657-2d6d-4e7b-a2fa-1a4cbe75178c';
BEGIN
    -- ---------------------------------------------------------
    -- A. Création du système EARNINGS
    -- ---------------------------------------------------------
    INSERT INTO "users" (id, username, first_name, last_name, email, password, role, status, auth_mode)
    VALUES (
        earnings_user_id, 'system_earnings', 'System', 'Earnings', 'earnings@forkeat.app',
        'SYSTEM_ACCOUNT_NO_LOGIN', 'ADMIN', 'ACTIVE', 'LOCAL'
    );

    INSERT INTO wallets (id, user_id, balance)
    VALUES (earnings_wallet_id, earnings_user_id, 0);

    INSERT INTO user_systems (id, system_type, user_id)
    VALUES (gen_random_uuid(), 'USER_WALLET_EARNINGS', earnings_user_id);

    INSERT INTO platform_wallets (id, wallet_id, type)
    VALUES (gen_random_uuid(), earnings_wallet_id, 'EARNINGS');

    -- ---------------------------------------------------------
    -- B. Création du système REDISTRIBUTION
    -- ---------------------------------------------------------
    INSERT INTO "users" (id, username, first_name, last_name, email, password, role, status, auth_mode)
    VALUES (
        redistribution_user_id, 'system_redistribution', 'System', 'Redistribution', 'redistribution@forkeat.app',
        'SYSTEM_ACCOUNT_NO_LOGIN', 'ADMIN', 'ACTIVE', 'LOCAL'
    );

    INSERT INTO wallets (id, user_id, balance)
    VALUES (redistribution_wallet_id, redistribution_user_id, 0);

    INSERT INTO user_systems (id, system_type, user_id)
    VALUES (gen_random_uuid(), 'USER_WALLET_REDISTRIBUTION', redistribution_user_id);

    INSERT INTO platform_wallets (id, wallet_id, type)
    VALUES (gen_random_uuid(), redistribution_wallet_id, 'REDISTRIBUTION');

    -- ---------------------------------------------------------
    -- C. Migration des recettes et suppression de l'ancien utilisateur
    -- ---------------------------------------------------------

    -- Réassigner toutes les recettes de l'ancien utilisateur vers 'system_earnings'
    UPDATE recipes SET author_id = earnings_user_id WHERE author_id = old_user_id;

    -- Supprimer l'ancien utilisateur (le wallet associé sera supprimé en cascade)
    DELETE FROM "users" WHERE id = old_user_id;

END $$;
