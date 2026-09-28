-- ============================================
-- ENUMS - Types personnalisés pour PostgresSQL
-- ============================================

-- Mode d'authentification de l'utilisateur
CREATE TYPE "auth_mode" AS ENUM (
	'LOCAL',
	'GOOGLE'
);

-- Type de transaction financière
CREATE TYPE "transaction_type" AS ENUM (
	'RECHARGE',
	'SUPER_LIKE',
	'REDISTRIBUTION'
);

-- Statut d'un utilisateur
CREATE TYPE "user_status" AS ENUM (
	'ACTIVE',
	'SUSPENDED',
	'BANNED'
);

-- Statut d'une recette dans le workflow de publication
CREATE TYPE "recipe_status" AS ENUM (
	'DRAFT',
	'PENDING_REVIEW',
	'PUBLISHED',
	'REJECTED'
);

-- Statut d'un signalement (recipe ou user)
CREATE TYPE "report_status" AS ENUM (
	'PENDING',
	'DISMISSED',
	'VALIDATED'
);

-- Type de signalement d'une recette
CREATE TYPE "recipe_report_type" AS ENUM (
	'DANGEROUS',
	'INAPPROPRIATE',
	'ALLERGENS',
	'COPYRIGHT',
	'SPAM',
	'OTHER'
);

-- Type de signalement d'un utilisateur
CREATE TYPE "user_report_type" AS ENUM (
	'SPAM',
	'HARASSMENT',
	'INAPPROPRIATE_CONTENT',
	'FRAUD',
	'OTHER'
);

-- Action de modération sur une recette
CREATE TYPE "recipe_moderation_action_type" AS ENUM (
	'REJECTED',
	'APPROVED'
);

-- Action de modération sur un utilisateur
CREATE TYPE "user_moderation_action_type" AS ENUM (
	'SUSPENDED',
	'BANNED',
	'WARNING'
);

-- Type de portefeuille de la plateforme
CREATE TYPE "type_wallet" AS ENUM (
	'EARNINGS',
	'REDISTRIBUTION'
);

-- Type de promotion
CREATE TYPE "type_promotion" AS ENUM (
	'PRICE_REDUCTION',
	'BONUS'
);

-- Statut d'une promotion
CREATE TYPE "promotion_status" AS ENUM (
	'SCHEDULED',
	'ACTIVE',
	'COMPLETED'
);

-- Profils utilisateur du système
CREATE TYPE "user_system_type" AS ENUM (
    'USER_WALLET_EARNINGS',
    'USER_WALLET_REDISTRIBUTION',
    'USER_RECIPE_PLATFORM'
);

-- Rôles des utilisateurs
CREATE TYPE "user_role" AS ENUM (
    'MEMBER',
    'MODERATOR',
    'ADMIN'
);

-- Niveau de gravité d'un allergène
CREATE TYPE "allergen_severity" AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

-- ============================================
-- TABLES - Schéma principal de la base de données
-- ============================================

-- Table des utilisateurs
CREATE TABLE IF NOT EXISTS "user" (
	"id" UUID NOT NULL,
    "username" VARCHAR(100) NOT NULL UNIQUE,
	"first_name" VARCHAR(100) NOT NULL,
	"last_name" VARCHAR(100) NOT NULL,
	"email" VARCHAR(255) NOT NULL UNIQUE,
	"password" VARCHAR(255) NOT NULL,
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"role" user_role NOT NULL DEFAULT 'MEMBER',
	"status" user_status NOT NULL DEFAULT 'ACTIVE',
	"auth_mode" auth_mode NOT NULL,
	PRIMARY KEY("id")
);

-- Table des portefeuilles utilisateur
CREATE TABLE IF NOT EXISTS "wallet" (
	"id" UUID NOT NULL,
	"user_id" UUID NOT NULL UNIQUE,
	"balance" BIGINT NOT NULL DEFAULT 0,
	"updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	PRIMARY KEY("id"),
	FOREIGN KEY("user_id") REFERENCES "user"("id") ON DELETE CASCADE
);

-- Table des transactions financières
CREATE TABLE IF NOT EXISTS "transaction" (
	"id" UUID NOT NULL,
	"source_wallet_id" UUID,
	"destination_wallet_id" UUID,
	"amount" BIGINT NOT NULL,
	"type" transaction_type NOT NULL,
	"stripe_transaction_id" VARCHAR(255),
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	PRIMARY KEY("id"),
	FOREIGN KEY("source_wallet_id") REFERENCES "wallet"("id") ON DELETE SET NULL,
	FOREIGN KEY("destination_wallet_id") REFERENCES "wallet"("id") ON DELETE SET NULL
);

-- Table des informations bancaires
CREATE TABLE IF NOT EXISTS "bank_info" (
	"id" UUID NOT NULL,
	"user_id" UUID NOT NULL UNIQUE,
	"bank_name" VARCHAR(255),
	"iban" VARCHAR(50) NOT NULL,
	"bic" VARCHAR(50) NOT NULL,
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	PRIMARY KEY("id"),
	FOREIGN KEY("user_id") REFERENCES "user"("id") ON DELETE CASCADE
);

-- Table des portefeuilles de la plateforme
CREATE TABLE IF NOT EXISTS "platform_wallet" (
	"id" UUID NOT NULL,
	"wallet_id" UUID NOT NULL UNIQUE,
	"type" type_wallet NOT NULL,
	PRIMARY KEY("id"),
	FOREIGN KEY("wallet_id") REFERENCES "wallet"("id")
);

-- Table des utilisateurs système
CREATE TABLE IF NOT EXISTS "user_system" (
    "id" UUID NOT NULL,
    "system_type" user_system_type NOT NULL,
    "user_id" UUID NOT NULL,
    PRIMARY KEY("id"),
    UNIQUE("system_type", "user_id"),
    FOREIGN KEY("user_id") REFERENCES "user"("id")
);

-- Table des recettes
CREATE TABLE IF NOT EXISTS "recipe" (
	"id" UUID NOT NULL,
	"title" VARCHAR(255) NOT NULL,
	"summary" TEXT NOT NULL,
	"parent_id" UUID,
	"author_id" UUID NOT NULL,
	"preparation_minutes" INTEGER,
	"step_by_step_instructions" JSONB NOT NULL,
	"image_url" TEXT,
	"status" recipe_status NOT NULL DEFAULT 'DRAFT',
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"dietary_flag" JSONB NOT NULL,
	PRIMARY KEY("id"),
	FOREIGN KEY("author_id") REFERENCES "user"("id"),
	FOREIGN KEY("parent_id") REFERENCES "recipe"("id")
);

-- Table des signalements de recettes
CREATE TABLE IF NOT EXISTS "recipe_report" (
	"id" UUID NOT NULL,
	"recipe_id" UUID NOT NULL,
	"reporter_id" UUID NOT NULL,
	"report_type" recipe_report_type NOT NULL,
	"status" report_status NOT NULL DEFAULT 'PENDING',
	"justification" TEXT NOT NULL,
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"reviewed_at" TIMESTAMP,
	"reviewed_by" UUID,
	PRIMARY KEY("id"),
	FOREIGN KEY("recipe_id") REFERENCES "recipe"("id"),
	FOREIGN KEY("reporter_id") REFERENCES "user"("id") ON DELETE SET NULL,
	FOREIGN KEY("reviewed_by") REFERENCES "user"("id") ON DELETE SET NULL
);

-- Table des actions de modération sur les recettes
CREATE TABLE IF NOT EXISTS "recipe_moderation_action" (
	"id" UUID NOT NULL,
	"recipe_id" UUID NOT NULL,
	"moderator_id" UUID,
	"action_type" recipe_moderation_action_type NOT NULL,
	"justification" TEXT NOT NULL,
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"related_report_id" UUID,
	PRIMARY KEY("id"),
	FOREIGN KEY("recipe_id") REFERENCES "recipe"("id") ON DELETE CASCADE,
	FOREIGN KEY("moderator_id") REFERENCES "user"("id") ON DELETE SET NULL,
	FOREIGN KEY("related_report_id") REFERENCES "recipe_report"("id") ON DELETE SET NULL
);

-- Table des signalements d'utilisateurs
CREATE TABLE IF NOT EXISTS "user_report" (
	"id" UUID NOT NULL,
	"reported_user_id" UUID NOT NULL,
	"reporter_id" UUID NOT NULL,
	"report_type" user_report_type NOT NULL,
	"status" report_status NOT NULL DEFAULT 'PENDING',
	"justification" TEXT NOT NULL,
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"reviewed_at" TIMESTAMP,
	"reviewed_by" UUID,
	PRIMARY KEY("id"),
	FOREIGN KEY("reported_user_id") REFERENCES "user"("id") ON DELETE CASCADE,
	FOREIGN KEY("reporter_id") REFERENCES "user"("id") ON DELETE SET NULL,
	FOREIGN KEY("reviewed_by") REFERENCES "user"("id") ON DELETE SET NULL
);

-- Table des actions de modération sur les utilisateurs
CREATE TABLE IF NOT EXISTS "user_moderation_action" (
	"id" UUID NOT NULL,
	"user_id" UUID NOT NULL,
	"moderator_id" UUID,
	"action_type" user_moderation_action_type NOT NULL,
	"justification" TEXT NOT NULL,
	"created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
	"suspended_until" TIMESTAMP,
	"related_report_id" UUID,
	PRIMARY KEY("id"),
	FOREIGN KEY("user_id") REFERENCES "user"("id") ON DELETE CASCADE,
	FOREIGN KEY("moderator_id") REFERENCES "user"("id") ON DELETE SET NULL,
	FOREIGN KEY("related_report_id") REFERENCES "user_report"("id") ON DELETE SET NULL
);

-- Table des ingrédients
CREATE TABLE IF NOT EXISTS "ingredient" (
	"id" UUID NOT NULL UNIQUE,
	"name" VARCHAR(255) NOT NULL,
	"category" VARCHAR(255) NOT NULL,
	"is_allergen" BOOLEAN,
	PRIMARY KEY("id")
);

-- Table de liaison recette-ingrédient
CREATE TABLE IF NOT EXISTS "recipe_ingredient" (
	"id" UUID NOT NULL UNIQUE,
	"recipe_id" UUID NOT NULL,
	"ingredient_id" UUID NOT NULL,
	"quantity" DECIMAL,
	"unit" VARCHAR(30),
	PRIMARY KEY("id"),
	UNIQUE("recipe_id", "ingredient_id"),
	FOREIGN KEY("recipe_id") REFERENCES "recipe"("id") ON DELETE CASCADE,
	FOREIGN KEY("ingredient_id") REFERENCES "ingredient"("id")
);

-- Table des allergènes
CREATE TABLE IF NOT EXISTS "allergen" (
	"id" UUID NOT NULL UNIQUE,
	"name" VARCHAR(255) NOT NULL,
	"severity" allergen_severity NOT NULL DEFAULT 'LOW',
	PRIMARY KEY("id")
);

-- Table de liaison recette-allergène
CREATE TABLE IF NOT EXISTS "recipe_allergen" (
    "id" UUID NOT NULL UNIQUE,
	"recipe_id" UUID NOT NULL,
	"allergen_id" UUID NOT NULL,
    PRIMARY KEY("id"),
	UNIQUE("recipe_id", "allergen_id"),
	FOREIGN KEY("recipe_id") REFERENCES "recipe"("id") ON DELETE CASCADE,
	FOREIGN KEY("allergen_id") REFERENCES "allergen"("id")
);

-- Table des promotions
CREATE TABLE IF NOT EXISTS "promotion" (
	"id" UUID NOT NULL UNIQUE,
	"start_date" DATE NOT NULL,
	"end_date" DATE NOT NULL,
	"type" type_promotion NOT NULL,
	"status" promotion_status NOT NULL,
	"original_price" DECIMAL NOT NULL,
	"new_price" DECIMAL NOT NULL,
    "minimum_super_likes" INTEGER,
	"created_at" DATE NOT NULL,
	"created_by" UUID NOT NULL,
	PRIMARY KEY("id"),
	FOREIGN KEY("created_by") REFERENCES "user"("id") ON DELETE SET NULL
);

-- ============================================
-- INDEX - Optimisation des requêtes fréquentes
-- ============================================

-- Index pour les recherches d'utilisateurs
CREATE INDEX "idx_user_email" ON "user" ("email");
CREATE INDEX "idx_user_status" ON "user" ("status");

-- Index pour les transactions (requêtes fréquentes par wallet et date)
CREATE INDEX "idx_transaction_source" ON "transaction" ("source_wallet_id");
CREATE INDEX "idx_transaction_dest" ON "transaction" ("destination_wallet_id");
CREATE INDEX "idx_transaction_date" ON "transaction" ("created_at");

-- Index pour les recettes (recherche par statut, auteur, date)
CREATE INDEX "idx_recipe_status" ON "recipe" ("status");
CREATE INDEX "idx_recipe_author" ON "recipe" ("author_id");

-- Index pour les signalements (recherche par statut)
CREATE INDEX "idx_recipe_report_status" ON "recipe_report" ("status");
CREATE INDEX "idx_user_report_status" ON "user_report" ("status");

-- Index pour les ingrédients (recherche par nom)
CREATE INDEX "idx_ingredient_name" ON "ingredient" ("name");

-- Index pour les allergènes (recherche par nom)
CREATE INDEX "idx_allergen_name" ON "allergen" ("name");
