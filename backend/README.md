
## Démarrage Rapide

### Prérequis

- **Java 25** (JDK installé)
- **Maven**
- **Docker Desktop** (pour PostgreSQL et Neo4j)

### Installation

1. **Démarrer Docker Desktop**

2. **Démarrer les bases de données** (PostgreSQL + Neo4j)

```bash
docker-compose up -d
```
---

## Commandes Utiles

### Docker

```bash
# Voir les logs des conteneurs
docker-compose logs -f

# Arrêter les conteneurs
docker-compose down

# Supprimer les conteneurs ET les volumes (efface les données)
docker-compose down -v

# Redémarrer uniquement PostgreSQL
docker-compose restart postgres

# Redémarrer uniquement Neo4j
docker-compose restart neo4j
```
### Base de Données

```bash
# Se connecter à PostgreSQL (depuis le conteneur)
docker exec -it forkeat-postgres psql -U postgres -d forkeat_db

# Voir les migrations Flyway appliquées
SELECT * FROM flyway_schema_history;

# Ouvrir Neo4j Browser
# Aller sur http://localhost:7474
# Identifiants: neo4j / password
```

---
