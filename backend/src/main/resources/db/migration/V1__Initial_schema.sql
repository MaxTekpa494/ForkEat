-- ============================================
-- Enums
-- ============================================

CREATE TYPE "auth_mode" AS ENUM (
    'LOCAL',
    'GOOGLE'
);

CREATE TYPE "transaction_type" AS ENUM (
    'RECHARGE',
    'SUPER_LIKE',
    'REDISTRIBUTION'
);

CREATE TYPE "user_status" AS ENUM (
    'ACTIVE',
    'SUSPENDED',
    'BANNED'
);

CREATE TYPE "recipe_status" AS ENUM (
    'DRAFT',
    'PENDING_REVIEW',
    'PUBLISHED',
    'REJECTED'
);

CREATE TYPE "recipe_difficulty" AS ENUM (
    'EASY',
    'MEDIUM',
    'HARD'
);

CREATE TYPE "report_status" AS ENUM (
    'PENDING',
    'DISMISSED',
    'VALIDATED'
);

CREATE TYPE "recipe_report_type" AS ENUM (
    'DANGEROUS',
    'INAPPROPRIATE',
    'ALLERGENS',
    'COPYRIGHT',
    'SPAM',
    'OTHER'
);

CREATE TYPE "user_report_type" AS ENUM (
    'SPAM',
    'HARASSMENT',
    'INAPPROPRIATE_CONTENT',
    'FRAUD',
    'OTHER'
);

CREATE TYPE "recipe_moderation_action_type" AS ENUM (
    'REJECTED',
    'APPROVED'
);

CREATE TYPE "user_moderation_action_type" AS ENUM (
    'SUSPENDED',
    'BANNED',
    'WARNING'
);

CREATE TYPE "type_wallet" AS ENUM (
    'PROFIT',
    'REDISTRIBUTION'
);

-- ============================================
-- Tables principales
-- ============================================

CREATE TABLE IF NOT EXISTS "users" (
    "id" UUID NOT NULL,
    "first_name" VARCHAR(100) NOT NULL,
    "last_name" VARCHAR(100) NOT NULL,
    "email" VARCHAR(255) NOT NULL UNIQUE,
    "password" VARCHAR(255) NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "role" VARCHAR(50) NOT NULL,
    "status" user_status NOT NULL DEFAULT 'ACTIVE',
    "auth_mode" auth_mode NOT NULL,
    "current_moderation_action_id" UUID,
    PRIMARY KEY("id")
);

CREATE INDEX "idx_user_email" ON "users" ("email");
CREATE INDEX "idx_user_status" ON "users" ("status");

CREATE TABLE IF NOT EXISTS "wallet" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL UNIQUE,
    "balance" INTEGER NOT NULL DEFAULT 0 CHECK (balance >= 0),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY("id")
);

CREATE INDEX "idx_wallet_user" ON "wallet" ("user_id");

CREATE TABLE IF NOT EXISTS "transaction" (
    "id" UUID NOT NULL,
    "source_wallet_id" UUID,
    "destination_wallet_id" UUID,
    "amount" INTEGER NOT NULL,
    "type" transaction_type NOT NULL,
    "stripe_transaction_id" VARCHAR(255),
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY("id")
);

CREATE INDEX "idx_transaction_source" ON "transaction" ("source_wallet_id");
CREATE INDEX "idx_transaction_dest" ON "transaction" ("destination_wallet_id");
CREATE INDEX "idx_transaction_date" ON "transaction" ("created_at");
CREATE INDEX "idx_transaction_type" ON "transaction" ("type");

CREATE TABLE IF NOT EXISTS "bank_info" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL UNIQUE,
    "bank_name" VARCHAR(255),
    "iban" VARCHAR(34) NOT NULL,
    "bic" VARCHAR(11) NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY("id")
);

CREATE INDEX "idx_bank_info_user" ON "bank_info" ("user_id");

CREATE TABLE IF NOT EXISTS "platform_wallet" (
    "id" UUID NOT NULL,
    "wallet_id" UUID NOT NULL UNIQUE,
    "type" type_wallet NOT NULL,
    PRIMARY KEY("id")
);

CREATE INDEX "idx_platform_wallet" ON "platform_wallet" ("wallet_id");
CREATE INDEX "idx_platform_wallet_type" ON "platform_wallet" ("type");

CREATE TABLE IF NOT EXISTS "recipe" (
    "id" UUID NOT NULL,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT,
    "author_id" UUID NOT NULL,
    "parent_id" UUID,
    "status" recipe_status NOT NULL DEFAULT 'DRAFT',
    "ingredients" JSONB NOT NULL DEFAULT '[]',
    "preparation_time_minutes" INTEGER,
    "cooking_time_minutes" INTEGER,
    "servings" INTEGER DEFAULT 4,
    "difficulty" recipe_difficulty DEFAULT 'MEDIUM',
    "photo_url" TEXT,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "current_moderation_action_id" UUID,
    PRIMARY KEY("id")
);

CREATE INDEX "idx_recipe_status" ON "recipe" ("status");
CREATE INDEX "idx_recipe_author" ON "recipe" ("author_id");
CREATE INDEX "idx_recipe_parent" ON "recipe" ("parent_id");
CREATE INDEX "idx_recipe_created" ON "recipe" ("created_at");

CREATE TABLE IF NOT EXISTS "recipe_step" (
    "id" UUID NOT NULL,
    "recipe_id" UUID NOT NULL,
    "step_order" INTEGER NOT NULL,
    "instruction" TEXT NOT NULL,
    "duration_minutes" INTEGER,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY("id")
);

CREATE INDEX "idx_recipe_step_recipe" ON "recipe_step" ("recipe_id");
CREATE INDEX "idx_recipe_step_order" ON "recipe_step" ("recipe_id", "step_order");

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
    PRIMARY KEY("id")
);

CREATE INDEX "idx_recipe_report_recipe" ON "recipe_report" ("recipe_id");
CREATE INDEX "idx_recipe_report_status" ON "recipe_report" ("status");
CREATE INDEX "idx_recipe_report_reporter" ON "recipe_report" ("reporter_id");

CREATE TABLE IF NOT EXISTS "recipe_moderation_action" (
    "id" UUID NOT NULL,
    "recipe_id" UUID NOT NULL,
    "moderator_id" UUID,
    "action_type" recipe_moderation_action_type NOT NULL,
    "justification" TEXT NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "related_report_id" UUID,
    PRIMARY KEY("id")
);

CREATE INDEX "idx_recipe_moderation_recipe" ON "recipe_moderation_action" ("recipe_id");
CREATE INDEX "idx_recipe_moderation_moderator" ON "recipe_moderation_action" ("moderator_id");
CREATE INDEX "idx_recipe_moderation_type" ON "recipe_moderation_action" ("action_type");

CREATE TABLE IF NOT EXISTS "user_report" (
    "id" UUID NOT NULL,
    "reported_user_id" UUID NOT NULL,
    "reporter_id" UUID NOT NULL,
    "report_type" user_report_type NOT NULL,
    "status" report_status NOT NULL DEFAULT 'PENDING',
    "justification" TEXT NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "reviewed_at" TIMESTAMP,
    "reviewed_by" UUID,
    PRIMARY KEY("id")
);

CREATE INDEX "idx_user_report_reported" ON "user_report" ("reported_user_id");
CREATE INDEX "idx_user_report_status" ON "user_report" ("status");
CREATE INDEX "idx_user_report_reporter" ON "user_report" ("reporter_id");

CREATE TABLE IF NOT EXISTS "user_moderation_action" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "moderator_id" UUID,
    "action_type" user_moderation_action_type NOT NULL,
    "justification" TEXT NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "suspended_until" TIMESTAMP,
    "related_report_id" UUID,
    PRIMARY KEY("id")
);

CREATE INDEX "idx_user_moderation_user" ON "user_moderation_action" ("user_id");
CREATE INDEX "idx_user_moderation_moderator" ON "user_moderation_action" ("moderator_id");
CREATE INDEX "idx_user_moderation_suspension" ON "user_moderation_action" ("suspended_until");

-- ============================================
-- Foreign Keys (ordre important pour éviter les erreurs)
-- ============================================

-- Wallet
ALTER TABLE "wallet"
ADD FOREIGN KEY("user_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

-- Bank Info
ALTER TABLE "bank_info"
ADD FOREIGN KEY("user_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

-- Transaction
ALTER TABLE "transaction"
ADD FOREIGN KEY("source_wallet_id") REFERENCES "wallet"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

ALTER TABLE "transaction"
ADD FOREIGN KEY("destination_wallet_id") REFERENCES "wallet"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

-- Platform Wallet
ALTER TABLE "platform_wallet"
ADD FOREIGN KEY("wallet_id") REFERENCES "wallet"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

-- Recipe
ALTER TABLE "recipe"
ADD FOREIGN KEY("author_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "recipe"
ADD FOREIGN KEY("parent_id") REFERENCES "recipe"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

-- Recipe Step
ALTER TABLE "recipe_step"
ADD FOREIGN KEY("recipe_id") REFERENCES "recipe"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

-- Recipe Moderation Action
ALTER TABLE "recipe_moderation_action"
ADD FOREIGN KEY("recipe_id") REFERENCES "recipe"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "recipe_moderation_action"
ADD FOREIGN KEY("moderator_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

ALTER TABLE "recipe_moderation_action"
ADD FOREIGN KEY("related_report_id") REFERENCES "recipe_report"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

-- Recipe Report
ALTER TABLE "recipe_report"
ADD FOREIGN KEY("recipe_id") REFERENCES "recipe"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "recipe_report"
ADD FOREIGN KEY("reporter_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

ALTER TABLE "recipe_report"
ADD FOREIGN KEY("reviewed_by") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

-- User Moderation Action
ALTER TABLE "user_moderation_action"
ADD FOREIGN KEY("user_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "user_moderation_action"
ADD FOREIGN KEY("moderator_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

ALTER TABLE "user_moderation_action"
ADD FOREIGN KEY("related_report_id") REFERENCES "user_report"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

-- User Report
ALTER TABLE "user_report"
ADD FOREIGN KEY("reported_user_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "user_report"
ADD FOREIGN KEY("reporter_id") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

ALTER TABLE "user_report"
ADD FOREIGN KEY("reviewed_by") REFERENCES "users"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

-- ============================================
-- Foreign Keys Circulaires (ajoutées en dernier)
-- Ces FK créent des références circulaires, mais c'est acceptable
-- car, les colonnes sont NULLABLE
-- ============================================

ALTER TABLE "users"
ADD FOREIGN KEY("current_moderation_action_id") REFERENCES "user_moderation_action"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;

ALTER TABLE "recipe"
ADD FOREIGN KEY("current_moderation_action_id") REFERENCES "recipe_moderation_action"("id")
ON UPDATE NO ACTION ON DELETE SET NULL;
