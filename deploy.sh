#!/bin/bash
# Script de déploiement du framework vers Tomcat
# Usage: ./deploy.sh [context-path]
#   context-path : nom du contexte (defaut: test-project)

set -e

FRAMEWORK_DIR="$(cd "$(dirname "$0")" && pwd)"
TOMCAT_HOME="/home/mikoja/tomcat/apache-tomcat-10.0.16"
CONTEXT_PATH="${1:-test-project}"

echo "============================================"
echo "  Framework - Deploiement vers Tomcat"
echo "============================================"
echo "Repertoire framework : $FRAMEWORK_DIR"
echo "Tomcat               : $TOMCAT_HOME"
echo "Contexte             : $CONTEXT_PATH"
echo ""

# 1. Construire le framework JAR
echo "[1/4] Construction du framework JAR..."
cd "$FRAMEWORK_DIR/framework"
mvn install -DskipTests -q
echo "  OK"

# 2. Construire le projet de test (WAR)
echo "[2/4] Construction du WAR..."
cd "$FRAMEWORK_DIR/test-project"
mvn package -DskipTests -q
echo "  OK"

# 3. Arreter Tomcat
echo "[3/4] Arret de Tomcat..."
"$TOMCAT_HOME/bin/shutdown.sh" 2>/dev/null || true
sleep 1

# 4. Copier le WAR et nettoyer l'ancien contexte
echo "[4/4] Deploiement du WAR..."
WAR_SRC="$FRAMEWORK_DIR/test-project/target/test-project-1.0.0.war"
WAR_DST="$TOMCAT_HOME/webapps/$CONTEXT_PATH.war"
CONTEXT_DIR="$TOMCAT_HOME/webapps/$CONTEXT_PATH"

# Supprimer l'ancien contexte deploye
if [ -d "$CONTEXT_DIR" ]; then
    echo "  Suppression de l'ancien contexte : $CONTEXT_DIR"
    rm -rf "$CONTEXT_DIR"
fi

if [ -f "$WAR_DST" ]; then
    echo "  Suppression de l'ancien WAR : $WAR_DST"
    rm -f "$WAR_DST"
fi

cp "$WAR_SRC" "$WAR_DST"
echo "  WAR copie vers : $WAR_DST"

# 5. Redemarrer Tomcat
echo "[5/5] Demarrage de Tomcat..."
"$TOMCAT_HOME/bin/startup.sh"
sleep 2

echo ""
echo "============================================"
echo "  Deploiement termine !"
echo "  URL : http://localhost:8080/$CONTEXT_PATH/"
echo "============================================"
