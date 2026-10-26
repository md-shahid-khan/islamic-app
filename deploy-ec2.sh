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

# Automatically sync sensitive environment variables if passed from CI/CD
update_env_key() {
    local key="$1"
    local val="$2"
    if [ -n "$val" ]; then
        if grep -q "^${key}=" .env; then
            sed -i "s|^${key}=.*|${key}=${val}|" .env
        else
            echo "${key}=${val}" >> .env
        fi
    fi
}

update_env_key "SPRING_MAIL_USERNAME" "${SPRING_MAIL_USERNAME:-}"
update_env_key "SPRING_MAIL_PASSWORD" "${SPRING_MAIL_PASSWORD:-}"
update_env_key "DB_PASSWORD" "${DB_PASSWORD:-}"
update_env_key "APP_JWT_SECRET" "${APP_JWT_SECRET:-}"

# Ensure docker compose is available
if ! command -v docker &> /dev/null; then
    echo "❌ ERROR: Docker is not installed on this EC2 instance."
    echo "Please install Docker and Docker Compose plugin first."
    exit 1
fi

echo "📥 [2/5] Pulling latest production Docker image..."
docker compose pull backend

echo "🔄 [3/5] Restarting services with zero downtime..."
# Clean up any conflicting foreign containers from previous manual or differently named runs
for c in angus_backend angus_nginx angus_db angus_redis; do
    if docker ps -a --format '{{.Names}}' | grep -Eq "^/?${c}\$"; then
        CURRENT_PROJ=$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "$c" 2>/dev/null || echo "")
        if [ "$CURRENT_PROJ" != "angusmasail" ]; then
            echo "⚠️ Removing conflicting container: $c (previous project: '${CURRENT_PROJ:-standalone}')"
            docker rm -f "$c" >/dev/null 2>&1 || true
        fi
    fi
done

if ! docker compose up -d --remove-orphans; then
    echo "⚠️ Compose conflict detected. Forcing cleanup of stale container names..."
    docker rm -f angus_db angus_redis angus_backend angus_nginx 2>/dev/null || true
    docker compose up -d --remove-orphans
fi

echo "⏳ [4/5] Waiting for backend to become healthy..."
MAX_ATTEMPTS=24
COUNT=0

while [ $COUNT -lt $MAX_ATTEMPTS ]; do
    if curl -s -f "http://127.0.0.1:8080/health" > /dev/null 2>&1 || curl -s -f "http://127.0.0.1/health" > /dev/null 2>&1; then
        echo "✅ Backend health check passed! (returned 200 OK)"
        docker compose exec -T -u root backend chown -R spring:spring /app/uploads >/dev/null 2>&1 || true
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
