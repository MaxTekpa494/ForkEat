# Architecture du Projet ForkEat - Rapport Technique

**Projet :** Plateforme de Partage de Recettes avec Rémunération  
**Équipe :** 5 développeurs  
**Durée :** 2,5 mois  
**Date :** Janvier 2026

---

## 1. Introduction

### Contraintes Architecturales

- **Gestion financière critique** : Transactions ACID strictes (Stripe, wallets, redistribution)
- **Double base de données** : PostgreSQL (données complètes) + Neo4j (graphe social)
- **Scalabilité** : Support d'un très grand nombre d'utilisateurs (critère d'évaluation)

### Solution Adoptée : Architecture en Couches "Super Clean"

- Séparation stricte métier / infrastructure
- Transparence de la double base de données
- Testabilité maximale
- Pragmatisme (livraison 2,5 mois)

---

## 2. Principes Architecturaux

- ✅ Modèles métier POJO purs (aucune annotation JPA/Neo4j)
- ✅ Interfaces définies côté métier (vocabulaire métier)
- ✅ Implémentations dans `infrastructure/` (Adapters)
- ✅ Inversion de dépendance (infrastructure → service)
- ✅ Vocabulaire familier (Service, Persistence)

---

## 3. Schéma d'Architecture

```
┌─────────────────────────────────────────────────────┐
│              COUCHE PRÉSENTATION                    │
│  REST API, MVC (Thymeleaf), DTOs                    │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              COUCHE SERVICE (Métier)                │
│  • Services (RecipeService, WalletService...)       │
│  • Modèles POJO (Recipe, User, Wallet...)           │
│  • Interfaces (RecipePersistence, PaymentGateway...)│
└──────────────────────┬──────────────────────────────┘
                       ▲ implémente
┌──────────────────────┴──────────────────────────────┐
│              COUCHE INFRASTRUCTURE                  │
│                                                     │
│  Persistence:                                       │
│  • adapter/ → RecipePersistenceAdapter              │
│  • postgres/ → Entities, Repositories, Mappers      │
│  • neo4j/ → Nodes, Relationships, Repositories      │
│  • sync/cdc/ → Debezium (nettoyage Neo4j)           │
│                                                     │
│  Stripe:                                            │
│  • adapter/ → StripePaymentGatewayAdapter           │
│                                                     │
│  Email:                                             │
│  • adapter/ → EmailNotificationAdapter              │
│                                                     │
│  LLM:                                               │
│  • adapter/ → LocalLLMAdapter                       │
└─────────────────────────────────────────────────────┘
```

**Direction des dépendances :**
```
Présentation → Service ← Infrastructure (Adapters)
```

---

## 4. Organisation des Packages

```
src/main/java/com/forkeat/
│
├── presentation/
│   ├── rest/
│   │   ├── controller/
│   │   ├── dto/
│   │   └── exception/               # Gestion exceptions REST (GlobalExceptionHandler)
│   └── web/controller/
│
├── service/
│   ├── RecipeService.java, UserService.java, WalletService.java...
│   ├── scheduled/                    # Jobs planifiés (CRON)
│   ├── model/                        # POJO métier (Recipe, User, Wallet...)
│   ├── persistence/                  # Interfaces (RecipePersistence...)
│   ├── external/                     # Interfaces (PaymentGateway, LLMService...)
│   └── exception/                    # Exceptions métier
│
└── infrastructure/
    ├── persistence/
    │   ├── adapter/                  # RecipePersistenceAdapter...
    │   ├── postgres/
    │   │   ├── entity/               # Entités JPA
    │   │   ├── repository/           # Spring Data JPA
    │   │   └── mapper/               # Model ↔ Entity
    │   ├── neo4j/
    │   │   ├── node/
    │   │   ├── relationship/
    │   │   └── repository/
    │   └── sync/cdc/                 # Debezium
    │
    ├── stripe/
    │   └── adapter/
    │
    ├── email/
    │   └── adapter/
    │
    ├── llm/
    │   └── adapter/
    │
    ├── security/
    └── config/
```

### Gestion des Exceptions

- **`service/exception/`** : Exceptions métier (RecipeNotFoundException, InsufficientFundsException...)
- **`presentation/rest/exception/`** : Gestion des exceptions REST
  - `@RestControllerAdvice` (GlobalExceptionHandler)
  - Mapping exceptions métier → réponses HTTP (404, 400, 500...)
  - Formatage JSON des erreurs

### Organisation des Adapters

Chaque domaine d'infrastructure contient son dossier `adapter/` :

```
service/persistence/RecipePersistence.java (INTERFACE)
                    ▲
                    │ implémente
infrastructure/persistence/adapter/RecipePersistenceAdapter.java
                    │
                    ├──► postgres/repository/
                    └──► neo4j/repository/
```

---

## 5. Gestion Transactionnelle

### Règle : @Transactional sur Service ET Adapters

- **SERVICE** : `@Transactional` (frontière métier)
- **ADAPTERS** : `@Transactional(REQUIRED)` (participe ou crée transaction)
- **Propagation REQUIRED** (défaut) : réutilise transaction existante

**Exemple :**
```
@Transactional
RecipeService.publishRecipe() {
    BEGIN Transaction T1
    ├─► recipePersistence.save(...)    → @Transactional(REQUIRED) → T1
    ├─► walletService.debit(...)       → @Transactional(REQUIRED) → T1
    COMMIT T1  // Transaction unique atomique
}
```

### Cas Particulier : Paiements Stripe avec 3D Secure

**Architecture : Frontend gère 3D Secure, Backend gère la capture**

#### Flux en 2 Phases

```
┌──────────────────────────────────────────────────────────────┐
│  FRONTEND (Navigateur)                                       │
├──────────────────────────────────────────────────────────────┤
│  1. Stripe.js crée PaymentMethod (carte)                     │
│  2. Stripe.js crée PaymentIntent avec capture_method=manual  │
│  3. Stripe.js confirme + 3D Secure (popup banque)            │
│     → Status: "requires_capture"                             │
│     → Argent BLOQUÉ (réservé) mais PAS débité                │
│  4. Envoie paymentIntentId au backend                        │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  BACKEND (Serveur)                                           │
├──────────────────────────────────────────────────────────────┤
│  5. Vérifie PaymentIntent.status = "requires_capture"        │
│  6. @Transactional BEGIN                                     │
│  7. Crédite wallet PostgreSQL                                │
│  8. Enregistre transaction                                   │
│  9. CAPTURE PaymentIntent (débit effectif)                   │
│ 10. COMMIT                                                   │
│                                                              │
│  OU en cas d'erreur PostgreSQL:                              │
│     → CANCEL PaymentIntent (libération immédiate)            │
│     → ROLLBACK PostgreSQL                                    │
└──────────────────────────────────────────────────────────────┘
```

#### Paramètre Crucial : capture_method = 'manual'

**Frontend** crée le PaymentIntent avec :
```javascript
stripe.createPaymentIntent({
    amount: 1000,  // 10€ en centimes
    currency: 'eur',
    capture_method: 'manual',  // ✅ CRITIQUE : permet capture différée
    confirm: true              // Confirme avec 3D Secure
})
```

**Backend** capture ou annule :
```java
// Si tout OK
paymentGateway.captureIntent(intentId);  // Débit effectif

// Si erreur BDD
paymentGateway.cancelIntent(intentId);   // Libération immédiate
```

#### Garanties de Sécurité

| Scénario | Résultat |
|----------|----------|
| **Tout réussit** | Wallet crédité + Argent débité (capture) |
| **PostgreSQL plante** | Rollback BDD + Cancel = Argent libéré immédiatement |
| **Serveur crash** | Argent bloqué 7 jours puis libéré automatiquement |
| **Cancel échoue** | Hold expire en 7 jours (libération automatique) |

#### Pire Cas : Serveur Crash

```
T0     : Frontend confirme → Argent BLOQUÉ (status: requires_capture)
T+1s   : Serveur crash avant capture/cancel
T+...  : Rien ne se passe (pas de capture, pas de cancel)
T+7j   : Stripe libère automatiquement l'argent (expiration)
```

**Conséquence** : Le client attend 7 jours, mais ne perd jamais son argent.

**Mitigation** : Job de nettoyage qui détecte les PaymentIntent orphelins et les annule.

---

## 6. Conclusion

### Avantages

| Aspect | Bénéfice |
|--------|----------|
| **Testabilité** | Services testables avec fakes (pas de BDD) |
| **Maintenabilité** | Changement tech = modification `infrastructure/` uniquement |
| **Scalabilité** | Neo4j pour graphe social O(k) vs SQL O(k×log n) |
| **Sécurité** | Transactions ACID + Stripe 2-Phase Commit |
| **Pragmatisme** | Vocabulaire familier, livraison délais respectés |

### Points d'Attention

```
⚠️ POINTS CRITIQUES

1. Transactions
   • @Transactional sur Service ET Adapters
   • Propagation REQUIRED
   • SELECT FOR UPDATE pour wallets

2. Paiements Stripe (capture manuelle + 3D Secure)
   • Frontend : Crée PaymentIntent avec capture_method='manual'
   • Frontend : Confirme avec 3D Secure → status='requires_capture'
   • Backend : Vérifie status, crédite wallet, puis CAPTURE
   • En cas d'erreur BDD : CANCEL (libération immédiate)
   • Pire cas (crash serveur) : Expiration auto en 7 jours

3. Séparation stricte
   • Modèles POJO ↔ Entités JPA toujours séparés
   • Mapping explicite (infrastructure/persistence/postgres/mapper/)
```

### Validation des Objectifs

| Objectif | Statut |
|----------|--------|
| Séparation métier/infra | ✅ POJO vs Entités JPA |
| Transparence double BDD | ✅ Adapters + CDC |
| Scalabilité | ✅ Neo4j graphe |
| Sécurité financière | ✅ ACID + Stripe Intent |
| Testabilité | ✅ Fakes mémoire |
| Maintenabilité | ✅ Impact localisé |

---

**Cette architecture garantit un code propre, testable et maintenable tout en respectant les contraintes du projet ForkEat.**