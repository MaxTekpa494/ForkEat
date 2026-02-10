/*
 * V7 - Email Verification System
 * Changes:
 * Add email_verified column to users table
 * Create verification_tokens table for email confirmation, password change, and email change flows
 */

-- Add email_verified column to users table
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing users are considered verified (grandfathered)
UPDATE users SET email_verified = TRUE;

-- Create verification_token_type enum
CREATE TYPE verification_token_type AS ENUM (
    'EMAIL_CONFIRMATION',
    'PASSWORD_CHANGE',
    'EMAIL_CHANGE'
);

-- Create verification_tokens table
CREATE TABLE verification_tokens (
    "id" UUID PRIMARY KEY,
    "user_id" UUID NOT NULL,
    "token" VARCHAR(255) NOT NULL,
    "type" verification_token_type NOT NULL,
    "payload" TEXT,
    "expires_at" TIMESTAMP NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_verification_token_user
        FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE,
    CONSTRAINT uq_verification_user_type
        UNIQUE ("user_id", "type")
);

-- Indexes
CREATE INDEX idx_verification_token ON verification_tokens ("token");
CREATE INDEX idx_verification_user_type ON verification_tokens ("user_id", "type");
