#!/usr/bin/env bash
# =========================================================
#  CABEX Externalisation — démarrage (Linux)
#  À lancer depuis le dossier d'installation (ex. /opt/cabex).
#  Contient : backend.jar + .env (+ dossier data/ créé au besoin)
# =========================================================
set -euo pipefail

cd "$(dirname "$0")"

# --- Vérification des prérequis
if ! command -v java >/dev/null 2>&1; then
  echo "ERREUR : Java n'est pas installé. Installer JDK 21+ (openjdk-21-jdk / Temurin)." >&2
  exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "${JAVA_VERSION:-0}" -lt 21 ]; then
  echo "ERREUR : Java 21+ requis (trouvé : ${JAVA_VERSION:-inconnue})." >&2
  exit 1
fi
if ! command -v mysql >/dev/null 2>&1 || ! command -v mysqldump >/dev/null 2>&1; then
  echo "AVERTISSEMENT : 'mysql' et/ou 'mysqldump' introuvables — la sauvegarde sera désactivée." >&2
fi

# --- Le dossier d'installation est le répertoire courant
mkdir -p data/documents data/backups

# --- Les variables d'environnement priment sur .env (config import par défaut)
#     (l'option spring.config.import=optional:file:./.env[.properties] est embarquée dans le jar)
PORT="${SERVER_PORT:-8080}"

echo "Démarrage de CABEX Externalisation sur le port $PORT…"
exec java -jar "backend.jar" --server.port="$PORT"
