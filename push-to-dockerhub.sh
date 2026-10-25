#!/usr/bin/env bash
set -e

DOCKER_USER="$1"
if [ -z "$DOCKER_USER" ]; then
    read -rp "Enter your Docker Hub username: " DOCKER_USER
fi

if [ -z "$DOCKER_USER" ]; then
    echo "[ERROR] Docker Hub username is required."
    exit 1
fi

echo ""
echo "[1/3] Tagging image as ${DOCKER_USER}/angusmasail-backend:latest ..."
docker tag angusmasail-backend:latest "${DOCKER_USER}/angusmasail-backend:latest"

echo ""
echo "[2/3] Logging in to Docker Hub ..."
docker login

echo ""
echo "[3/3] Pushing ${DOCKER_USER}/angusmasail-backend:latest to Docker Hub ..."
docker push "${DOCKER_USER}/angusmasail-backend:latest"

echo ""
echo "=============================================================================="
echo "[SUCCESS] Image pushed successfully!"
echo "Repository: ${DOCKER_USER}/angusmasail-backend:latest"
echo "On your AWS EC2 instance, set:"
echo "export DOCKER_IMAGE=${DOCKER_USER}/angusmasail-backend:latest"
echo "docker compose up -d"
echo "=============================================================================="
