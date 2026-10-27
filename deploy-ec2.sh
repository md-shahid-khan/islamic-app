#!/usr/bin/env bash
# ==============================================================================
# Angus Masail - Simple EC2 Production Deployment
# Usage: ./deploy-ec2.sh
# ==============================================================================
set -euo pipefail

APP_DIR="${APP_DIR:-$HOME/angusmasail}"
echo "============================================================"
echo "🚀 [1/5] Deploying Angus Masail in: $APP_DIR"
echo "============================================================"

mkdir -p "$APP_DIR"
cd "$APP_DIR"

# 1. Ensure .env exists
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "⚠️ .env not found, initializing from .env.example..."
        cp .env.example .env
    else
        echo "❌ ERROR: .env file missing in $APP_DIR"
        exit 1
    fi
fi

# 2. Sync any environment variables passed dynamically
set_env() {
    local k="$1"
    local v="$2"
    if [ -n "$v" ]; then
        if grep -q "^${k}=" .env; then
            sed -i "s|^${k}=.*|${k}=${v}|" .env
        else
            echo "${k}=${v}" >> .env
        fi
    fi
}

set_env "SPRING_PROFILES_ACTIVE" "${SPRING_PROFILES_ACTIVE:-prod}"
set_env "SPRING_DATASOURCE_URL" "${SPRING_DATASOURCE_URL:-}"
set_env "SPRING_DATASOURCE_USERNAME" "${SPRING_DATASOURCE_USERNAME:-}"
set_env "SPRING_DATASOURCE_PASSWORD" "${SPRING_DATASOURCE_PASSWORD:-}"
set_env "SPRING_DATA_REDIS_HOST" "${SPRING_DATA_REDIS_HOST:-}"
set_env "SPRING_DATA_REDIS_PORT" "${SPRING_DATA_REDIS_PORT:-}"
set_env "SPRING_DATA_REDIS_PASSWORD" "${SPRING_DATA_REDIS_PASSWORD:-}"
set_env "SPRING_MAIL_USERNAME" "${SPRING_MAIL_USERNAME:-}"
set_env "SPRING_MAIL_PASSWORD" "${SPRING_MAIL_PASSWORD:-}"
set_env "APP_JWT_SECRET" "${APP_JWT_SECRET:-}"

# 3. Pull latest prebuilt Docker image from Docker Hub
echo "📥 [2/5] Pulling latest production image..."
docker compose pull backend

# 4. Clean any legacy orphaned containers (including legacy local db/redis)
for c in angus_backend angus_nginx angus_db angus_redis; do
    if docker ps -a --format '{{.Names}}' | grep -Eq "^/?${c}\$"; then
        CURRENT_PROJ=$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "$c" 2>/dev/null || echo "")
        if [ "$CURRENT_PROJ" != "angusmasail" ] || [ "$c" = "angus_db" ] || [ "$c" = "angus_redis" ]; then
            echo "🧹 Removing legacy or conflicting container: $c"
            docker rm -f "$c" >/dev/null 2>&1 || true
        fi
    fi
done

# 5. Start containers with zero-downtime recreation
echo "🔄 [3/5] Starting containers with docker compose..."
docker compose up -d --remove-orphans

# 6. Health check verification
echo "⏳ [4/5] Verifying backend health..."
MAX_ATTEMPTS=24
COUNT=0

while [ $COUNT -lt $MAX_ATTEMPTS ]; do
    if curl -s -f "http://127.0.0.1:8080/health" > /dev/null 2>&1 || curl -s -f "http://127.0.0.1/health" > /dev/null 2>&1; then
        echo "✅ Backend is healthy and running!"
        docker compose exec -T -u root backend chown -R spring:spring /app/uploads >/dev/null 2>&1 || true
        break
    fi
    COUNT=$((COUNT + 1))
    echo "   Starting up... ($COUNT/$MAX_ATTEMPTS)"
    sleep 5
done

if [ $COUNT -eq $MAX_ATTEMPTS ]; then
    echo "⚠️ Warning: Healthcheck timed out after 120s. Container logs:"
    docker compose logs --tail=40 backend
    exit 1
fi

# 7. Clean old images
echo "🧹 [5/5] Cleaning old Docker image layers..."
docker image prune -f || true

echo "============================================================"
echo "🎉 Angus Masail is live and healthy!"
echo "============================================================"
