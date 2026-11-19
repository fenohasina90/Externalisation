# =========================================================
#  CABEX Externalisation — démarrage (Windows PowerShell)
#  Lancer depuis le dossier d'installation (ex. C:\cabex).
#  Contient : backend.jar + .env (+ dossier data\ créé au besoin)
# =========================================================
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# --- Vérification des prérequis
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Error "Java n'est pas installé. Installer JDK 21+ (Eclipse Temurin — winget install EclipseAdoptium.Temurin.21.JDK)."
    exit 1
}
$v = (& java -version 2>&1) -join "`n"
if ($v -notmatch '"(\d+)') { Write-Error "Version Java indéterminée."; exit 1 }
if ([int]$Matches[1] -lt 21) { Write-Error "Java 21+ requis."; exit 1 }

if (-not (Get-Command mysql -ErrorAction SilentlyContinue) -or -not (Get-Command mysqldump -ErrorAction SilentlyContinue)) {
    Write-Warning "'mysql' et/ou 'mysqldump' introuvables — la sauvegarde sera désactivée."
}

# --- Créer les dossiers de travail
New-Item -ItemType Directory -Force -Path 'data\documents', 'data\backups' | Out-Null

# --- Démarrer (le fichier .env dans ce dossier est chargé par le JAR)
$Port = $env:SERVER_PORT
if (-not $Port) { $Port = '8080' }
Write-Host "Démarrage de CABEX Externalisation sur le port $Port…"
java -jar (Join-Path $PSScriptRoot 'backend.jar') --server.port=$Port
