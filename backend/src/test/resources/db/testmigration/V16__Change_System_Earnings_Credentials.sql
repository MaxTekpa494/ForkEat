-- Mise à jour des informations de l'utilisateur système 'system_earnings'
UPDATE "users"
SET username = 'ForkEat',
    email = 'forkeat.noreply@gmail.com',
    password = '$2a$10$6JuFo6cmpqZIfZGqUlO2aulwPePLR67QJ8QdDlKkghrSqZjymf8Pa', --password123
    first_name = 'ForkEat',
    last_name = 'Team',
    role = 'MODERATOR'
WHERE username = 'system_earnings';;