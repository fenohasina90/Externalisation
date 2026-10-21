# Application web de gestion et de suivi des services d'externalisation

Application full-stack de digitalisation des prestations d'externalisation : sociétés clientes,
demandes multi-services, factures proforma, plans de paiement, échéances, paiements, documents
PDF, notifications, rapports, journal d'audit et sauvegardes de la base MySQL.

## Sommaire

- [Architecture](#architecture)
- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Installation rapide](#installation-rapide)
- [Installation par système d'exploitation](#installation-par-système-dexploitation)
  - [Linux](#linux)
  - [macOS](#macos)
  - [Windows](#windows)
- [Base de données](#base-de-données)
- [Configuration](#configuration)
- [Démarrage](#démarrage)
- [Sauvegardes](#sauvegardes)
- [Tests et CI](#tests-et-ci)
- [Dépannage](#dépannage)
- [Sécurité et production](#sécurité-et-production)

## Architecture

```text
Externalisation/
├── backend/          API REST Spring Boot / Java 21
├── frontend/         Interface React 19 / TypeScript / Vite
├── database/         Schéma MySQL final et migrations
└── .github/          Intégration continue (GitHub Actions)
```

- **Backend** : Spring Boot 4.1.1 (Web, Data JPA, Security, Validation), authentification JWT,
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

### Versions minimales

| Outil | Version | Rôle |
| --- | --- | --- |
| Java JDK | **21** | Compilation et exécution du backend |
| Node.js | **22.12 ou plus** | Compilation et exécution du frontend |
| npm | **10 ou plus** | Installation des dépendances frontend |
| MySQL Server | **8.0 ou plus** | Base de données de production/développement |
| Git | récente | Récupération du dépôt |
| `mysql` et `mysqldump` | même version majeure que MySQL | Restauration et sauvegarde |

> Le projet cible **Java 21** et **MySQL 8+**. MariaDB/XAMPP peut fonctionner en développement,
> mais la configuration de référence reste MySQL 8+. Les tests backend utilisent H2 et ne
> nécessitent pas MySQL.

Maven n'est pas à installer : le **Maven Wrapper** (`backend/mvnw` ou `backend/mvnw.cmd`) télécharge
la version adaptée au premier lancement. Une connexion Internet est donc nécessaire au premier
build.

### Vérification de l'environnement

À exécuter dans un terminal (PowerShell sous Windows) :

```bash
java -version
node --version
npm --version
mysql --version
mysqldump --version
```

`java -version` doit afficher une version **21**. Si plusieurs versions de Java sont installées,
vérifier que `JAVA_HOME` pointe vers le JDK 21.

### Ports utilisés

| Service | Port par défaut | Variable |
| --- | --- | --- |
| API Spring Boot | `8080` | `SERVER_PORT` |
| Frontend Vite | `5173` | `--port` de Vite |
| MySQL | `3306` | configuration MySQL |

Ces ports doivent être libres, ou adaptés dans la configuration.

## Installation rapide

Les commandes ci-dessous correspondent à **Linux/macOS**. Les équivalents Windows sont détaillés
dans la section [Windows](#windows).

```bash
# 1. Récupérer le projet
git clone <url-du-depot> Externalisation
cd Externalisation

# 2. Créer la base et importer le schéma
mysql -u root -p < database/BD_corrige.sql

# 3. Créer le fichier de configuration
cp .env.example .env
# Générer une clé JWT puis la reporter dans .env
openssl rand -base64 64 | tr -d '\n'
chmod 600 .env

# 4. Démarrer le backend (terminal 1)
cd backend
./mvnw spring-boot:run

# 5. Démarrer le frontend (terminal 2, depuis la racine)
cd frontend
npm ci
npm run dev
```

Accès :

- Frontend : `http://localhost:5173`
- API : `http://localhost:8080/api`
- Swagger : `http://localhost:8080/swagger-ui.html`

Au premier démarrage, si aucun utilisateur n'existe, un compte administrateur est créé
automatiquement : **`admin` / `admin123`**. Changez ce mot de passe immédiatement après la
première connexion.

## Installation par système d'exploitation

### Linux

Les commandes varient selon la distribution. Choisir la section correspondante.

#### Debian / Ubuntu

```bash
sudo apt update
sudo apt install -y git curl unzip openssl
sudo apt install -y openjdk-21-jdk
sudo apt install -y mysql-server mysql-client

# Node.js 22 via NodeSource
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs

# Démarrer MySQL
sudo systemctl enable --now mysql
sudo mysql_secure_installation
```

Si le paquet `openjdk-21-jdk` n'est pas disponible sur votre version de Debian/Ubuntu, installer
le JDK 21 depuis [Adoptium](https://adoptium.net/) ou avec SDKMAN.

#### Fedora / RHEL / Rocky Linux

```bash
sudo dnf install -y git curl unzip openssl
sudo dnf install -y java-21-openjdk-devel
sudo dnf install -y mysql-server mysql

# Node.js 22 via NodeSource
curl -fsSL https://rpm.nodesource.com/setup_22.x | sudo bash -
sudo dnf install -y nodejs

sudo systemctl enable --now mysqld
sudo mysql_secure_installation
```

#### Arch Linux / Manjaro

```bash
sudo pacman -S --needed git curl unzip openssl jdk21-openjdk nodejs npm mysql mysql-clients
sudo systemctl enable --now mysqld
sudo mysql_secure_installation
```

#### Vérification

```bash
java -version
node --version
mysql --version
mysqldump --version
```

Les binaires `mysql` et `mysqldump` sont fournis par `mysql-client` (Debian/Ubuntu),
`mysql` (Fedora) ou `mysql-clients` (Arch). Ils sont nécessaires au module de sauvegarde.

### macOS

Prérequis : macOS récent, Xcode Command Line Tools et [Homebrew](https://brew.sh/).

```bash
xcode-select --install

# Installer Homebrew si nécessaire
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

brew install git openjdk@21 mysql
```

Configurer Java 21 dans le shell (à ajouter à `~/.zshrc` pour le rendre permanent) :

```bash
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

Installer Node.js 22+ :

```bash
brew install node
node --version   # doit afficher 22.12 ou plus
```

Démarrer MySQL :

```bash
brew services start mysql
mysql_secure_installation
```

Sur Apple Silicon, Homebrew est généralement installé dans `/opt/homebrew` ; sur Intel, dans
`/usr/local`. Les commandes `brew --prefix` s'adaptent automatiquement.

Vérifier les binaires de sauvegarde :

```bash
which mysql
which mysqldump
```

### Windows

Deux approches sont possibles : **Windows natif** (PowerShell) ou **WSL2** avec une distribution
Linux. WSL2 est recommandée si vous êtes déjà à l'aise avec les commandes Linux.

#### Windows natif — PowerShell

Ouvrir PowerShell en administrateur pour les installations :

```powershell
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id OpenJS.NodeJS.LTS -e
winget install --id Oracle.MySQL -e
```

`Oracle.MySQL` installe MySQL Installer. Lancer ensuite **MySQL Installer**, choisir
**MySQL Server** et **MySQL Client**, définir le mot de passe `root`, puis conserver le port
`3306`. Si `winget` n'est pas disponible, télécharger les installateurs depuis les sites
officiels.

Ajouter le dossier `bin` de MySQL au `PATH` utilisateur, puis rouvrir PowerShell :

```powershell
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin"
[Environment]::SetEnvironmentVariable(
  "Path",
  [Environment]::GetEnvironmentVariable("Path", "User") + ";$mysqlBin",
  "User"
)
```

Adapter le chemin à la version installée. Avec **XAMPP**, utiliser plutôt :

```text
C:\xampp\mysql\bin
```

Vérifier l'environnement :

```powershell
java -version
node --version
npm --version
mysql --version
mysqldump --version
```

Dans PowerShell, utiliser `.\mvnw.cmd` à la place de `./mvnw`. Dans Git Bash, `./mvnw` fonctionne
comme sous Linux.

#### WSL2

```powershell
wsl --install -d Ubuntu
```

Redémarrer si nécessaire, puis suivre la section [Debian / Ubuntu](#debian--ubuntu) **à
l'intérieur de WSL2**. Installer et démarrer MySQL dans WSL2 pour éviter les problèmes de
connexion entre Windows et Linux. Si `systemctl` n'est pas disponible, utiliser
`sudo service mysql start`.

## Base de données

Le schéma de référence est `database/BD_corrige.sql`. Il crée la base
`gestion_externalisation`, les tables, les rôles, les permissions et les paramètres par défaut.

> **Attention** : `BD_corrige.sql` contient des `DROP TABLE`. Il est destiné à une installation
> neuve ou à une remise à zéro. Ne pas l'exécuter sur une base contenant des données à conserver.

### Import du schéma

Linux / macOS :

```bash
mysql -u root -p < database/BD_corrige.sql
```

Sur Debian/Ubuntu, le compte `root` utilise parfois l'authentification `auth_socket` : utiliser
alors `sudo mysql < database/BD_corrige.sql`, ou créer un utilisateur dédié (voir ci-dessous).

Windows PowerShell :

```powershell
cmd /c "mysql -u root -p < database\BD_corrige.sql"
```

Sous PowerShell, l'opérateur `<` n'est pas pris en charge directement : `cmd /c` est nécessaire.
Il est aussi possible d'utiliser MySQL Workbench (`Server > Data Import`).

### Utilisateur MySQL dédié (recommandé)

Éviter d'utiliser `root` pour l'application :

```sql
CREATE USER IF NOT EXISTS 'cabex'@'localhost' IDENTIFIED BY 'MotDePasseFort!2026';
GRANT ALL PRIVILEGES ON gestion_externalisation.* TO 'cabex'@'localhost';
FLUSH PRIVILEGES;
```

Puis renseigner `DB_USERNAME=cabex` et `DB_PASSWORD=...` dans `.env`.

### Base existante et migrations

Pour une base déjà installée, ne pas rejouer `BD_corrige.sql`. Appliquer les scripts de
`database/migrations/` **un par un, dans l'ordre chronologique**, puis redémarrer le backend :

```bash
mysql -u cabex -p gestion_externalisation < database/migrations/nom_de_la_migration.sql
```

Les migrations sont incrémentales et certaines supposent que les précédentes ont été appliquées.
Ne pas les exécuter avec un simple `for ... in database/migrations/*.sql` : l'ordre alphabétique
des noms de fichiers ne garantit pas l'ordre chronologique.

Avec `JPA_DDL_AUTO=validate` (valeur par défaut), Hibernate vérifie la cohérence entre les entités
et le schéma au démarrage, sans modifier la base. Une erreur de validation indique généralement
un schéma incomplet ou une migration manquante.

## Configuration

Les valeurs sensibles sont lues dans l'environnement : **aucun secret ne doit être commité**.
Le backend charge automatiquement `backend/.env` (lorsqu'il est lancé depuis `backend/`) et le
`.env` de la racine du dépôt. Les variables d'environnement du système restent prioritaires.
Éviter de définir une même clé dans les deux fichiers `.env`.

En production, préférer les variables d'environnement ou un coffre-fort de secrets.

### Créer le fichier `.env`

Linux / macOS :

```bash
cp .env.example .env
chmod 600 .env
```

Windows PowerShell :

```powershell
Copy-Item .env.example .env
```

Éditer ensuite `.env` avec un éditeur (`nano`, `vim`, VS Code, Notepad++, etc.).

### Générer `JWT_SECRET`

La clé doit être une chaîne **Base64** représentant au moins **32 octets** (256 bits). Une clé de
64 octets est recommandée.

Linux / macOS :

```bash
openssl rand -base64 64 | tr -d '\n'
```

Windows PowerShell :

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 64
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Copier le résultat dans `.env`, sur la ligne `JWT_SECRET=...`. Ne jamais committer `.env` ni
partager cette clé.

### Variables backend

| Variable | Obligatoire | Valeur par défaut | Description |
| --- | --- | --- | --- |
| `JWT_SECRET` | **Oui** | — | Clé Base64 d'au moins 32 octets (256 bits) |
| `JWT_EXPIRATION_MS` | Non | `3600000` | Durée de repli des jetons (1 h) |
| `DB_URL` | Non | `jdbc:mysql://localhost:3306/gestion_externalisation` | URL JDBC MySQL |
| `DB_USERNAME` | Non | `root` | Utilisateur MySQL |
| `DB_PASSWORD` | Non | vide | Mot de passe MySQL |
| `JPA_DDL_AUTO` | Non | `validate` | `validate` en production ; `update` réservé au développement local |
| `CORS_ALLOWED_ORIGINS` | Non | `http://localhost:3000,http://localhost:5173,http://127.0.0.1:5173` | Origines autorisées, séparées par des virgules |
| `SERVER_PORT` | Non | `8080` | Port HTTP du backend |
| `BACKUP_DIR` | Non | `./data/backups` | Dossier des archives de sauvegarde, relatif au répertoire de lancement |
| `BACKUP_RETENTION_DAYS` | Non | `14` | Durée de rétention des archives |
| `BACKUP_HEURE` | Non | `02:00` | Heure par défaut de la sauvegarde automatique (modifiable dans l'administration) |
| `BACKUP_DB_HOST` | Non | `127.0.0.1` | Hôte MySQL utilisé par `mysqldump`/`mysql` |
| `BACKUP_DB_PORT` | Non | `3306` | Port MySQL utilisé par `mysqldump`/`mysql` |
| `BACKUP_DB_NAME` | Non | `gestion_externalisation` | Base sauvegardée/restaurée |
| `BACKUP_DB_USERNAME` | Non | valeur de `DB_USERNAME` | Utilisateur MySQL des sauvegardes |
| `BACKUP_DB_PASSWORD` | Non | valeur de `DB_PASSWORD` | Mot de passe MySQL des sauvegardes |
| `MYSQLDUMP_PATH` | Non | `mysqldump` | Chemin complet de `mysqldump` si absent du `PATH` |
| `MYSQL_PATH` | Non | `mysql` | Chemin complet du client `mysql` si absent du `PATH` |
| `BACKUP_UPLOAD_MAX_SIZE` | Non | `268435456` | Taille maximale d'un fichier de restauration (octets) |

Paramètres complémentaires modifiables dans `backend/src/main/resources/application.properties`
ou par argument au lancement :

- `app.document.storage-path` : dossier des pièces jointes (défaut `./data/documents`) ;
- `app.document.max-size` : taille maximale d'un document (défaut `10485760`, soit 10 Mo) ;
- `app.mail.enabled` : activation de l'envoi d'e-mails (désactivé par défaut) ;
- `spring.mail.*` : serveur SMTP si `app.mail.enabled=true`.

### Exemple de `.env`

```dotenv
JWT_SECRET=remplacer-par-la-cle-generee
JWT_EXPIRATION_MS=3600000

DB_URL=jdbc:mysql://localhost:3306/gestion_externalisation
DB_USERNAME=cabex
DB_PASSWORD=MotDePasseFort!2026
JPA_DDL_AUTO=validate
CORS_ALLOWED_ORIGINS=http://localhost:5173

BACKUP_DIR=./data/backups
BACKUP_RETENTION_DAYS=14
BACKUP_HEURE=02:00

# Chemins explicites si les binaires MySQL ne sont pas dans le PATH
# MYSQLDUMP_PATH=/usr/bin/mysqldump
# MYSQL_PATH=/usr/bin/mysql
```

Sous Windows avec XAMPP, utiliser des chemins avec des barres obliques :

```dotenv
MYSQLDUMP_PATH=C:/xampp/mysql/bin/mysqldump.exe
MYSQL_PATH=C:/xampp/mysql/bin/mysql.exe
```

### Variables d'environnement (alternative au `.env`)

Linux / macOS :

```bash
export JWT_SECRET="votre-cle-base64"
export DB_USERNAME="cabex"
export DB_PASSWORD="MotDePasseFort!2026"
export CORS_ALLOWED_ORIGINS="http://localhost:5173"
```

Windows PowerShell (session courante) :

```powershell
$env:JWT_SECRET = "votre-cle-base64"
$env:DB_USERNAME = "cabex"
$env:DB_PASSWORD = "MotDePasseFort!2026"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
```

Pour conserver une variable après fermeture du terminal :

```powershell
setx JWT_SECRET "votre-cle-base64"
```

Rouvrir ensuite PowerShell. La variable `JWT_SECRET` doit être disponible pour le processus qui
lance le backend.

### Configuration du frontend

Le frontend utilise `VITE_API_URL` (valeur par défaut : `http://localhost:8080/api`). Pour changer
l'URL de l'API, créer `frontend/.env.local` :

```dotenv
VITE_API_URL=http://localhost:8080/api
```

Ce fichier est ignoré par Git. Si le backend écoute sur un autre port, mettre à jour à la fois
`VITE_API_URL` et `CORS_ALLOWED_ORIGINS`.

## Démarrage

Le backend et le frontend se lancent dans **deux terminaux séparés**.

### Backend

Linux / macOS :

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Le fichier `.env` de la racine (ou de `backend/`) est chargé automatiquement. L'API écoute sur
`http://localhost:8080`. La documentation OpenAPI est disponible sur
`http://localhost:8080/swagger-ui.html`.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

Windows PowerShell : mêmes commandes.

L'interface est disponible sur `http://localhost:5173`. Pour changer le port :

```bash
npm run dev -- --port 5174
```

Dans ce cas, ajouter `http://localhost:5174` à `CORS_ALLOWED_ORIGINS`.

### Premier compte

Au premier démarrage, si la table `utilisateur` est vide, un compte `SUPER_ADMIN` est créé :

- identifiant : `admin`
- mot de passe : `admin123`

Ce mot de passe doit être changé immédiatement après la première connexion. En production,
prévoir également la création des comptes `ACCUEIL` et `ECONOMAT` selon les besoins.

### Build de production

Backend :

```bash
cd backend
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Windows :

```powershell
cd backend
.\mvnw.cmd clean package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

Frontend :

```bash
cd frontend
npm ci
npm run build
```

Le dossier `frontend/dist/` contient les fichiers statiques à servir par un serveur web
(Nginx, Apache, IIS, etc.). Définir `VITE_API_URL` **avant** le build si l'API n'est pas servie
sur `http://localhost:8080/api`.

## Sauvegardes

Le module de sauvegarde pilote les clients officiels `mysqldump` et `mysql`. Ils doivent être
installés sur la machine qui héberge le backend.

| Système | Paquet / emplacement |
| --- | --- |
| Debian/Ubuntu | `mysql-client` |
| Fedora/RHEL | `mysql` |
| Arch | `mysql-clients` |
| macOS | formule Homebrew `mysql` |
| Windows | MySQL Installer (`...\MySQL Server 8.0\bin`) ou XAMPP (`C:\xampp\mysql\bin`) |

Si les binaires ne sont pas dans le `PATH`, renseigner `MYSQLDUMP_PATH` et `MYSQL_PATH` dans
`.env`. La sauvegarde automatique est déclenchée chaque jour à l'heure définie par
`HEURE_SAUVEGARDE_AUTOMATIQUE` (par défaut `02:00`, heure du serveur, modifiable dans
l'administration), lorsque l'application est en cours d'exécution. Une sauvegarde manuelle reste
disponible depuis l'écran Administration > Sauvegardes.

## Tests et CI

Backend (H2 en mémoire, aucun MySQL requis) :

```bash
cd backend

# Linux / macOS
./mvnw test

# Windows PowerShell
.\mvnw.cmd test
```

Frontend :

```bash
cd frontend
npm ci
npm run lint
npm test
npm run build
```

Le workflow GitHub Actions `.github/workflows/ci.yml` exécute ces vérifications sur les push vers
`main`/`develop` et sur chaque pull request.

## Dépannage

| Symptôme | Cause probable | Solution |
| --- | --- | --- |
| `JWT_SECRET` manquant ou invalide au démarrage | `.env` absent, clé non Base64 ou trop courte | Générer une clé de 64 octets et la placer dans `.env` |
| `Access denied for user ...` | Identifiants MySQL incorrects | Vérifier `DB_USERNAME` / `DB_PASSWORD` et les droits de l'utilisateur |
| `Communications link failure` | MySQL arrêté ou port incorrect | Démarrer MySQL et vérifier `DB_URL` / le port `3306` |
| `Unknown database 'gestion_externalisation'` | Schéma non importé | Exécuter `mysql -u root -p < database/BD_corrige.sql` |
| Erreur Hibernate `validate` au démarrage | Schéma incomplet ou migration manquante | Importer `BD_corrige.sql` ou appliquer les migrations |
| `mysqldump` / `mysql` introuvable | Binaires absents du `PATH` | Installer le client MySQL et/ou renseigner `MYSQLDUMP_PATH` et `MYSQL_PATH` |
| `./mvnw: Permission denied` | Wrapper non exécutable | `chmod +x mvnw` |
| `The '<' operator is reserved` sous PowerShell | Redirection non supportée | Utiliser `cmd /c "mysql ... < fichier.sql"` |
| Port `8080` déjà utilisé | Une autre instance tourne | Arrêter l'instance ou définir `SERVER_PORT` |
| Erreur CORS dans le navigateur | Origine frontend non autorisée | Ajouter l'URL exacte à `CORS_ALLOWED_ORIGINS` |
| `npm ci` échoue | `package-lock.json` absent/incohérent ou Node trop ancien | Utiliser Node 22.12+ et vérifier `frontend/package-lock.json` |

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
- Ne pas utiliser `JPA_DDL_AUTO=update` en production.
- Protéger `.env` : `chmod 600` sous Linux/macOS, droits restreints sous Windows.
