-- Mise à jour des informations de l'utilisateur système 'system_earnings'
UPDATE "users"
SET username = 'ForkEat',
    email = 'forkeat.noreply@gmail.com',
    password = '${system_earnings_password_hash}',
    first_name = 'ForkEat',
    last_name = 'Team',
    role = 'MODERATOR'
WHERE username = 'system_earnings';