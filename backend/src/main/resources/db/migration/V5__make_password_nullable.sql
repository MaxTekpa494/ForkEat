-- Rendre la colonne password nullable pour supporter OAuth2
ALTER TABLE users
ALTER COLUMN password DROP NOT NULL;