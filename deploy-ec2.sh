#!/usr/bin/env bash
# ==============================================================================
# Angus Masail - Automated EC2 Deployment Script
# Executed on AWS EC2 by GitHub Actions or manually via SSH
# ==============================================================================
set -euo pipefail

APP_DIR="${APP_DIR:-$HOME/angusmasail}"
echo "============================================================"
echo "🚀 [1/5] Starting Angus Masail Deployment in $APP_DIR"
echo "============================================================"

mkdir -p "$APP_DIR"
cd "$APP_DIR"

# Ensure .env file exists
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "⚠️ .env not found, initializing from .env.example..."
        cp .env.example .env
    else
        echo "❌ ERROR: .env file missing in $APP_DIR"
        exit 1
    fi
fi

# Ensure docker compose is available
if ! command -v docker &> /dev/null; then
    echo "❌ ERROR: Docker is not installed on this EC2 instance."
    echo "Please install Docker and Docker Compose plugin first."
    exit 1
fi

echo "📥 [2/5] Pulling latest production Docker image..."
docker compose pull backend

echo "🔄 [3/5] Restarting services with zero downtime..."
docker compose up -d --remove-orphans

echo "⏳ [4/5] Waiting for backend to become healthy..."
MAX_ATTEMPTS=24
COUNT=0
HEALTH_URL="http://127.0.0.1:8080/health"

while [ $COUNT -lt $MAX_ATTEMPTS ]; do
    if curl -s -f "$HEALTH_URL" > /dev/null 2>&1; then
        echo "✅ Backend health check passed! ($HEALTH_URL returned 200 OK)"
        break
    fi
    COUNT=$((COUNT + 1))
    echo "   Waiting for backend to start ($COUNT/$MAX_ATTEMPTS)..."
    sleep 5
done

if [ $COUNT -eq $MAX_ATTEMPTS ]; then
    echo "⚠️ Warning: Backend healthcheck timed out after 120s."
    echo "Inspecting last 50 lines of container logs:"
    docker compose logs --tail=50 backend
    exit 1
fi

echo "🧹 [5/5] Pruning old unused Docker images..."
docker image prune -f || true

echo "============================================================"
echo "🎉 DEPLOYMENT SUCCESSFUL!"
echo "Angus Masail backend is live and healthy."
echo "============================================================"
