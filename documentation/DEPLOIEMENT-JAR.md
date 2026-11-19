# Déploiement de l'application avec le JAR (installation sur un autre PC)

Objectif : installer **CABEX Externalisation** sur un PC Linux ou Windows en mode **tout-en-un** :
un seul fichier `backend.jar` contient **l'API Spring Boot et le frontend React** (fichiers
statiques compilés et servis par le serveur). Aucune installation de Node.js n'est nécessaire sur
la machine cible.

```
navigateur  ──►  http://IP:8080  ──►  backend.jar  ──►  MySQL
                              (API + interface)
```

---

## 1. Prérequis communs (toutes plateformes)

| Élément | Version | Remarque |
| --- | --- | --- |
| **Java JDK** | **21 ou plus** | Pas de JRE — le JAR est construit et exécuté sous Java 21 |
| **MySQL Server** | **8.0 ou plus** | Base `gestion_externalisation` |
| **Client MySQL** | même version majeure | `mysql` et `mysqldump` dans le `PATH` — requis pour les sauvegardes |
| **Système** | Linux (Debian/Ubuntu/Fedora…) ou Windows 10/11 ou Windows Server | 64 bits |

Mémoire recommandée : **2 Go minimum** (4 Go conseillés), **2 Go d'espace disque**.
Réseau : le PC doit être joignable sur le port **8080** (ou celui configuré).

---

## 2. Construire le JAR (une fois, sur la machine de développement)

La machine de build doit avoir Node.js 22+ et Maven (ou le wrapper).

```bash
cd Externalisation

# 1. Compiler le frontend
cd frontend
npm ci
npm run build           # produit frontend/dist/

# 2. Copier le frontend dans les ressources statiques du backend
mkdir -p ../backend/src/main/resources/static
cp -r dist/* ../backend/src/main/resources/static/

# 3. Construire le JAR (sans lancer les tests si vous êtes pressé)
cd ../backend
./mvnw clean package -DskipTests

# JAR produit : backend/target/backend-0.0.1-SNAPSHOT.jar
```

> **Windows** : remplacer `./mvnw` par `.\mvnw.cmd`.
> **Important** : le frontend est *empaqueté dans le JAR* ; toute modification de l'interface
> reconstruit les deux étapes.

---

## 3. Installer sur le PC cible

### 3.1. Préparer le dossier d'installation

Créer un dossier dédié, par exemple `/opt/cabex` (Linux) ou `C:\cabex` (Windows) :

```bash
# Linux
sudo mkdir -p /opt/cabex
sudo chown -R $USER:$USER /opt/cabex
```

```powershell
# Windows
New-Item -ItemType Directory -Force -Path 'C:\cabex'
```

Y déposer :
- `backend.jar` (renommé depuis `backend-0.0.1-SNAPSHOT.jar`) ;
- `.env` (voir section 4) ;
- `linux-start.sh` / `windows-start.ps1` (copiés depuis `scripts/` du dépôt).

```
/opt/cabex/
├── backend.jar
├── .env
├── linux-start.sh
└── data/            # créé automatiquement au 1er démarrage (documents + sauvegardes)
```

### 3.2. Linux — Debian / Ubuntu

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk mysql-server mysql-client

sudo systemctl enable --now mysql
sudo mysql_secure_installation
sudo mysql -e "CREATE DATABASE IF NOT EXISTS gestion_externalisation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql < database/BD_corrige.sql    # ou migration_2026_11_19_*.sql pour une base existante

# Utilisateur MySQL dédié (recommandé)
sudo mysql <<'SQL'
CREATE USER IF NOT EXISTS 'cabex'@'localhost' IDENTIFIED BY 'MotDePasseFort!2026';
GRANT ALL PRIVILEGES ON gestion_externalisation.* TO 'cabex'@'localhost';
FLUSH PRIVILEGES;
SQL

# Copier les fichiers
cp backend/target/backend-0.0.1-SNAPSHOT.jar /opt/cabex/backend.jar
cp .env /opt/cabex/.env
cp scripts/linux-start.sh /opt/cabex/linux-start.sh
chmod +x /opt/cabex/linux-start.sh
```

### 3.3. Linux — Fedora / RHEL

```bash
sudo dnf install -y java-21-openjdk-devel mysql-server mysql

sudo systemctl enable --now mysqld
sudo mysql_secure_installation
sudo mysql -e "CREATE DATABASE IF NOT EXISTS gestion_externalisation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql < database/BD_corrige.sql
sudo mysql <<'SQL'
CREATE USER IF NOT EXISTS 'cabex'@'localhost' IDENTIFIED BY 'MotDePasseFort!2026';
GRANT ALL PRIVILEGES ON gestion_externalisation.* TO 'cabex'@'localhost';
FLUSH PRIVILEGES;
SQL

sudo cp backend/target/backend-0.0.1-SNAPSHOT.jar /opt/cabex/backend.jar
sudo cp .env /opt/cabex/.env
sudo cp scripts/linux-start.sh /opt/cabex/linux-start.sh
sudo chmod +x /opt/cabex/linux-start.sh
```

### 3.4. Linux — Arch

```bash
sudo pacman -S --needed jdk21-openjdk mysql mysql-clients
sudo systemctl enable --now mysqld
sudo mysql_secure_installation
sudo mysql -e "CREATE DATABASE IF NOT EXISTS gestion_externalisation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql < database/BD_corrige.sql
sudo mysql <<'SQL'
CREATE USER IF NOT EXISTS 'cabex'@'localhost' IDENTIFIED BY 'MotDePasseFort!2026';
GRANT ALL PRIVILEGES ON gestion_externalisation.* TO 'cabex'@'localhost';
FLUSH PRIVILEGES;
SQL

sudo cp backend/target/backend-0.0.1-SNAPSHOT.jar /opt/cabex/backend.jar
sudo cp .env /opt/cabex/.env
sudo cp scripts/linux-start.sh /opt/cabex/linux-start.sh
sudo chmod +x /opt/cabex/linux-start.sh
```

### 3.5. Windows

**Option A — WSL2 (recommandée)** : installez Ubuntu dans WSL2 (`wsl --install -d Ubuntu`),
puis suivez la procédure **Linux** dans la distribution.

**Option B — Windows natif (PowerShell en administrateur)** :

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id Oracle.MySQL -e
# Suivre l'assistant MySQL Installer : installer MySQL Server + MySQL Client, mot de passe root.

# Ajouter le dossier bin MySQL au PATH utilisateur
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin"
[Environment]::SetEnvironmentVariable("Path",
  [Environment]::GetEnvironmentVariable("Path", "User") + ";" + $mysqlBin, "User")
# puis rouvrir PowerShell

# Créer la base
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS gestion_externalisation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
cmd /c "mysql -u root -p < database\BD_corrige.sql"
# Créer l'utilisateur applicatif
mysql -u root -p -e "CREATE USER IF NOT EXISTS 'cabex'@'localhost' IDENTIFIED BY 'MotDePasseFort!2026'; GRANT ALL PRIVILEGES ON gestion_externalisation.* TO 'cabex'@'localhost'; FLUSH PRIVILEGES;"

# Copier les fichiers
New-Item -ItemType Directory -Force -Path 'C:\cabex'
Copy-Item backend\target\backend-0.0.1-SNAPSHOT.jar C:\cabex\backend.jar
Copy-Item .env C:\cabex\.env
Copy-Item scripts\windows-start.ps1 C:\cabex\windows-start.ps1
```

---

## 4. Configuration (.env)

Le fichier `.env` est lu **depuis le répertoire d'exécution** (le JAR embarque
`spring.config.import=optional:file:./.env[.properties]`). Il se place donc à côté de `backend.jar` :

```dotenv
JWT_SECRET=<clé générée>
JWT_EXPIRATION_MS=86400000        # 24 h (3600000 = 1 h en dev)

DB_URL=jdbc:mysql://localhost:3306/gestion_externalisation
DB_USERNAME=cabex
DB_PASSWORD=MotDePasseFort!2026
JPA_DDL_AUTO=validate             # validate en production — jamais update
CORS_ALLOWED_ORIGINS=http://IP-ou-domaine:8080

# Sauvegardes (chemin vers les clients MySQL si absents du PATH)
BACKUP_DIR=./data/backups
BACKUP_RETENTION_DAYS=14
BACKUP_HEURE=02:00
# MYSQLDUMP_PATH=/usr/bin/mysqldump
# MYSQL_PATH=/usr/bin/mysql
```

Générer `JWT_SECRET` (clé Base64 ≥ 32 octets, en pratique 64) :

```bash
# Linux
openssl rand -base64 64 | tr -d '\n'
# Windows PowerShell
$b = New-Object byte[] 64; (New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($b); [Convert]::ToBase64String($b)
```

> **Ne jamais partager** `.env`. Sous Linux : `chmod 600 .env`. Sous Windows : compte dédié ou
> ACL restrictive.

> Si la machine cible n'a **pas les clients MySQL** (`mysql`/`mysqldump`), les sauvegardes
> (Administration → Sauvegardes) seront inopérantes, mais l'application fonctionnera.

---

## 5. Démarrer l'application

Linux :

```bash
cd /opt/cabex
./linux-start.sh
```

Windows (PowerShell) :

```powershell
cd C:\cabex
.\windows-start.ps1
```

Accès : `http://localhost:8080` (ou `http://IP-DU-PC:8080` sur le réseau local).

### 5.1. Linux — démarrer automatiquement au boot (`systemd`)

```bash
sudo cp scripts/linux-cabex.service /etc/systemd/system/cabex.service
# éditer User= et WorkingDirectory= si nécessaire
sudo systemctl daemon-reload
sudo systemctl enable --now cabex
sudo journalctl -u cabex -f
```

### 5.2. Windows — démarrer automatiquement au boot (NSSM)

```powershell
winget install --id NSSM.NSSM -e
nssm install CabexExternalisation "C:\Program Files\Java\jdk-21\bin\java.exe" "-jar" "C:\cabex\backend.jar" "--server.port=8080"
nssm set CabexExternalisation AppDirectory "C:\cabex"
nssm set CabexExternalisation AppStdout "C:\cabex\data\logs\cabex.log"
nssm set CabexExternalisation AppStderr "C:\cabex\data\logs\cabex.err.log"
Start-Service CabexExternalisation
```

(Alternative simple : une tâche planifiée « au démarrage » `java -jar C:\cabex\backend.jar`.)

---

## 6. Vérification post-installation

1. Ouvrir `http://IP-DU-PC:8080` dans le navigateur.
2. Se connecter avec `admin` / `admin123`, **changer le mot de passe** dans Profil.
3. Créer un client de test, une demande, valider une facture PDF, encaisser un paiement.
4. Tester une sauvegarde (Administration → Sauvegardes) et vérifier que `data/backups/` se
   remplit.
5. (Linux) `systemctl status cabex` — (Windows) `Get-Service CabexExternalisation`.

---

## 7. Dépannage rapide

| Symptôme | Cause | Solution |
| --- | --- | --- |
| `JWT_SECRET` invalide | `.env` absent ou clé non Base64/en trop peu d'octets | Régénérer la clé, relancer |
| `Communications link failure` | MySQL arrêté ou port/host incorrect | `systemctl start mysql` ; vérifier `DB_URL` |
| `Access denied` | mauvais utilisateur/mot de passe MySQL | vérifier `DB_USERNAME` / `DB_PASSWORD` et les GRANT |
| `Unknown database` | schéma non importé | importer `BD_corrige.sql` |
| 404 / page blanche sur toutes les routes | cible ne sert pas le bon JAR | reconstruire avec le frontend embarqué, recopier `index.html` |
| Erreur SQL Hibernate (`validate`) | schéma incomplet / migration manquante | appliquer les migrations `database/migrations/*.sql` dans l'ordre |
| Port 8080 déjà utilisé | une autre instance tourne | `SERVER_PORT=8081 ./linux-start.sh` |
| `mysqldump` introuvable | client MySQL absent du PATH | installer `mysql-client` (ou éditer `MYSQLDUMP_PATH` / `MYSQL_PATH`) |
| Erreur CORS | Front consulté depuis une autre URL que celle du backend | `CORS_ALLOWED_ORIGINS=http://IP-DU-PC:8080` puis relancer |

---

## 8. Sécurité / production

- `JPA_DDL_AUTO=validate` **seulement**.
- `JWT_SECRET` différent par déploiement, jamais partagé, jamais commité.
- Restriction réseau : ouvrir uniquement le port nécessaire (pare-feu, pare-feu Windows) —
  idéalement derrière un reverse proxy HTTPS (nginx, IIS+ARR, Traefik).
- Restreindre l'accès SSH/RDP de la machine.
- Sauvegarde quotidienne (fonction incluse) + rétention 14 jours par défaut. Tester régulièrement
  la **restauration**.
- Changer le mot de passe `admin` au premier login, créer les comptes `ACCUEIL` et `ECONOMAT`.
- Mettre à jour la JVM/MySQL régulièrement ; garder `.env` hors des sauvegardes non chiffrées.

---

## 9. Mise à jour / montée de version

1. Sauvegarder la base actuelle (Administration → Sauvegardes ou `mysqldump`).
2. Construire le nouveau JAR (section 2).
3. Appliquer les migrations manquantes de `database/migrations/` **dans l'ordre chronologique**.
4. Arrêter l'application, remplacer `backend.jar` par le nouveau, relancer.
5. Vérifier la connexion et quelques écrans ; consulter les logs (`journalctl -u cabex` /
   `data/logs/cabex.log`).
