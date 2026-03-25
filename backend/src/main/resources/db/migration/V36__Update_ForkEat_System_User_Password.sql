-- Mise à jour du mot de passe de l'utilisateur système 'ForkEat'
UPDATE "users"
SET password = '${system_earnings_password_hash}'
WHERE username = 'ForkEat';