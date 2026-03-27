# Projet Recettes UGE

Projet de recettes en Spring Boot, avec client léger (Spring MVC) et client lourd (Android).

Membres du groupe :

- Adel Bennouar : adel.bennouar@edu.univ-eiffel.fr
- Sid Ali Cherrati : sid-ali.cherrati@edu.univ-eiffel.fr
- Max Tekpa : max.tekpa@edu.univ-eiffel.fr
- Thierno Sy : thierno.sy@edu.univ-eiffel.fr
- Adel Ziani : adel.ziani@edu.univ.eiffel.fr

## Version en production

L'application est déployée et accessible à l'adresse suivant : https://forkeat.app

## Lancement du projet en local :

1. Clonez le projet.
2. À la racine, dupliquez le fichier `.env.example` et renommez-le en `.env`.
3. Ouvrez `.env` et renseignez les variables nécessaires. Pour obtenir le contenu du fichier `.env`, contactez l'équipe projet.
4. Lancez `docker compose up -d`.
5. Lancez le backend avec `mvn clean spring-boot:run`.
6. Pour les paiements du portefeuille (rechargement et retrait), lancez Stripe CLI avec la commande suivante :
   ```bash
   stripe listen --forward-to localhost:8080/api/wallet/webhooks/stripe
   ```

## Ports utilisés :

Les ports suivants seront occupés lors du lancement du projet :

- **8080** : API backend Spring Boot
- **5432** : PostgreSQL
- **7474** : Interface web Neo4j
- **7687** : Neo4j
- **11435** : Ollama
