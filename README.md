# Application web de gestion et de suivi des services d'externalisation

Application full-stack de digitalisation des prestations d'externalisation : sociétés clientes,
demandes multi-services, factures proforma, plans de paiement, échéances, paiements, documents
PDF, notifications, rapports, journal d'audit et sauvegardes de la base MySQL.

## Sommaire

- [Architecture](#architecture)
- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Configuration](#configuration)
- [Base de données](#base-de-données)
- [Démarrage](#démarrage)
- [Tests et CI](#tests-et-ci)
- [Documentation](#documentation)
- [Sécurité et production](#sécurité-et-production)

## Architecture

```text
Externalisation/
├── backend/          API REST Spring Boot / Java 21
├── frontend/         Interface React 19 / TypeScript / Vite
├── database/         Schéma MySQL final et migrations
├── documentation/    Cahier des charges, vues, tests API, sauvegardes, déploiement
└── .github/          Intégration continue (GitHub Actions)
```

- **Backend** : Spring Boot (Web, Data JPA, Security, Validation), authentification JWT,
  contrôle d'accès par permissions (`SUPER_ADMIN`, `ADMIN`, `ACCUEIL`, `ECONOMAT`).
- **Frontend** : React 19, React Router 7, Axios, navigation et écrans filtrés par permissions.
- **Base de données** : MySQL 8+ (H2 en mémoire pour les tests backend).
- **API** : REST, documentée avec Swagger/OpenAPI (`/swagger-ui.html`, `/v3/api-docs`).

## Fonctionnalités

- Authentification par identifiant + mot de passe (BCrypt) et jetons JWT.
- Gestion des utilisateurs, rôles et permissions, avec effets immédiats sans reconnexion.
- Référentiels : clients, responsables, commerciaux, domaines et services.
- Demandes de service multi-lignes, cycle de vie contrôlé et historique.
- Factures proforma `UNIQUE` ou `PAR_SERVICE`, validation et PDF.
- Plans de paiement, échéances, paiements, reçus et alertes.
- Documents, notifications, tableau de bord, rapports (PDF/Excel/CSV), recherche globale et audit.
- Sauvegarde/restauration MySQL : dump gzip, chiffrement AES-256-GCM optionnel, planification et
  rétention.

## Prérequis

- Java 21
- Node.js 22+ et npm
- MySQL 8+
- `mysqldump` et `mysql` installés pour le module de sauvegarde (`mysql-client` sous Debian/Ubuntu)

## Configuration

Les valeurs sensibles sont lues dans l'environnement : **aucun secret ne doit être commité**.
En local, le backend charge automatiquement le fichier `.env` placé à la racine du dépôt (ou
dans `backend/`) ; en production, utiliser les variables d'environnement ou un coffre-fort de
secrets. Le fichier `.env` est ignoré par Git.

| Variable | Obligatoire | Valeur par défaut | Description |
| --- | --- | --- | --- |
| `JWT_SECRET` | Oui | — | Clé Base64 d'au moins 32 octets (256 bits) |
| `JWT_EXPIRATION_MS` | Non | `3600000` | Durée de repli des jetons (1 h) |
| `DB_URL` | Non | `jdbc:mysql://localhost:3306/gestion_externalisation` | URL JDBC MySQL |
| `DB_USERNAME` | Non | `root` | Utilisateur MySQL |
| `DB_PASSWORD` | Non | vide | Mot de passe MySQL |
| `JPA_DDL_AUTO` | Non | `validate` | `validate` en production ; `update` réservé au développement local |
| `CORS_ALLOWED_ORIGINS` | Non | `http://localhost:3000,http://localhost:5173,http://127.0.0.1:5173` | Origines autorisées, séparées par des virgules |
| `BACKUP_DIR` | Non | `./data/backups` | Dossier des archives de sauvegarde |
| `BACKUP_RETENTION_DAYS` | Non | `14` | Durée de rétention |
| `BACKUP_CRON` | Non | `0 0 2 * * *` | Planification des sauvegardes automatiques |
| `BACKUP_DB_*` | Non | valeurs de la datasource | Connexion utilisée par `mysqldump`/`mysql` |
| `MYSQLDUMP_PATH`, `MYSQL_PATH` | Non | `mysqldump`, `mysql` | Chemins des clients MySQL |

Initialiser la configuration locale :

```bash
cp .env.example .env
JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" .env
chmod 600 .env
```

Ou exporter directement la clé dans le shell :

```bash
export JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
```

En production, définir également `CORS_ALLOWED_ORIGINS` avec le domaine exact du frontend.

## Base de données

Le schéma de référence est `database/BD_corrige.sql`. Il crée la base
`gestion_externalisation` et les 26 tables de l'application.

```bash
mysql -u root -p < database/BD_corrige.sql
```

Les évolutions incrémentales sont dans `database/migrations/`. Les appliquer dans l'ordre
chronologique sur une base existante.

Avec `JPA_DDL_AUTO=validate` (valeur par défaut), Hibernate vérifie la cohérence entre les entités
et le schéma au démarrage, sans modifier la base.

## Démarrage

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Le fichier `.env` de la racine est chargé automatiquement. En production, fournir les variables
d'environnement (`JWT_SECRET`, `DB_*`, `CORS_ALLOWED_ORIGINS`, etc.).

L'API écoute sur `http://localhost:8080`. La documentation OpenAPI est disponible sur
`http://localhost:8080/swagger-ui.html`.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

L'interface est disponible sur `http://localhost:5173`. L'URL de l'API peut être surchargée avec
`VITE_API_URL`.

## Tests et CI

```bash
# Backend : tests unitaires et d'intégration (H2)
cd backend
./mvnw test

# Frontend : lint, tests unitaires et build de production
cd frontend
npm ci
npm run lint
npm test
npm run build
```

Le workflow GitHub Actions `.github/workflows/ci.yml` exécute ces vérifications sur chaque push et
chaque pull request.

## Documentation

- [Index de la documentation](documentation/README.md)
- [Cahier des charges](documentation/CAHIER_DES_CHARGES.md)
- [Vues et parcours](documentation/VUES_APPLICATION.md)
- [Tests de l'API](documentation/API_TESTS.md)
- [Sauvegardes](documentation/SAUVEGARDES.md)
- [Fonctionnement des sauvegardes](documentation/FONCTIONNEMENT_SAUVEGARDE.md)
- [Déploiement et production](documentation/DEPLOIEMENT.md)

## Sécurité et production

- Le secret JWT est **obligatoire via `JWT_SECRET`** ; il est validé au démarrage (Base64, 256 bits
  minimum). Ne jamais le committer ni le placer dans une URL.
- `spring.jpa.hibernate.ddl-auto=validate` par défaut : le schéma est géré par les scripts SQL,
  jamais par Hibernate en production.
- CORS limité aux origines configurées ; renseigner le domaine de production via
  `CORS_ALLOWED_ORIGINS`.
- Les mots de passe sont hachés avec BCrypt ; les permissions sont relues en base à chaque requête.
- Les actions sensibles (authentification, CRUD, paiements, sauvegardes) sont tracées dans
  `audit_log`.
- Servir l'application derrière HTTPS et restreindre l'accès à la base et aux binaires MySQL.
