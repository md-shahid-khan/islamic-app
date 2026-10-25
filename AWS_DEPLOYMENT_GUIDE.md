# AWS EC2 Deployment Guide with Nginx Reverse Proxy & Docker

This guide walks you through deploying the **Angus Masail** backend on an AWS EC2 instance using Docker, Docker Compose, and Nginx reverse proxy.

---

## 1. Prerequisites & AWS Security Group Configuration

In the AWS EC2 Management Console, open your instance's **Security Group** and add the following **Inbound Rules**:

| Type | Protocol | Port Range | Source | Purpose |
|------|----------|------------|--------|---------|
| **HTTP** | TCP | `80` | `0.0.0.0/0` (Anywhere IPv4) | Web traffic & SSL challenge |
| **HTTPS** | TCP | `443` | `0.0.0.0/0` (Anywhere IPv4) | Secure API & WebSockets |
| **SSH** | TCP | `22` | `My IP` (Recommended) | Secure server administration |

> **Note:** The backend port `8080`, PostgreSQL `5432`, and Redis `6379` stay strictly **internal** to the Docker bridge network. They are not exposed to the public internet, ensuring maximum production security.

---

## 2. Push Image to Docker Hub (From Local Machine)

Run the automated push script on your development machine:

### Windows (PowerShell or CMD):
```bat
.\push-to-dockerhub.bat <your-dockerhub-username>
```

### Linux / Mac / WSL:
```bash
chmod +x push-to-dockerhub.sh
./push-to-dockerhub.sh <your-dockerhub-username>
```

*(You will be prompted to log in to Docker Hub if you haven't already).*

---

## 3. Server Setup on AWS EC2 (Ubuntu / Debian)

Connect to your AWS EC2 instance via SSH:
```bash
ssh -i your-key.pem ubuntu@your-ec2-public-ip
```

### Install Docker & Docker Compose:
```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# Add Docker's official GPG key & repository
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow current user to run docker commands
sudo usermod -aG docker $USER
newgrp docker
```

---

## 4. Deploy Angus Masail Application

### Option A: Clone Repository (Recommended)
```bash
git clone <your-repository-url> angusmasail
cd angusmasail
```

### Option B: Minimal Production Directory
If you only want to copy the deployment files to your EC2 instance:
1. `docker-compose.yml`
2. `docker/nginx/nginx.conf`
3. `docker/nginx/conf.d/default.conf`
4. `docker/nginx/ssl/server.crt` & `docker/nginx/ssl/server.key`

---

## 5. Launch Services

Set your Docker Hub image environment variable and start the containers:

```bash
# Export your Docker image
export DOCKER_IMAGE=<your-dockerhub-username>/angusmasail-backend:latest

# Export production secrets (Optional, has sensible defaults)
export DB_PASSWORD=your_secure_db_password
export APP_JWT_SECRET=your_super_secret_jwt_key_at_least_32_characters

# Start all services (PostgreSQL, Redis, Spring Boot Backend, Nginx)
docker compose up -d
```

Check the status:
```bash
docker compose ps
```

Verify backend logs:
```bash
docker compose logs -f backend
```

---

## 6. Verification

Your application is now live and accessible through Nginx:
- **Health / Imams API:** `http://<ec2-ip-or-domain>/api/v1/imams`
- **Swagger Documentation:** `http://<ec2-ip-or-domain>/swagger-ui/index.html`
- **WebSocket Chat / Notifications:** `ws://<ec2-ip-or-domain>/ws-chat` or `wss://...`
- **File Uploads (50MB support):** `http://<ec2-ip-or-domain>/uploads/...`

---

## 7. Adding Free Let's Encrypt SSL (Certbot)

If you have a domain pointed to your EC2 instance (e.g. `api.yourdomain.com`):

```bash
sudo docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    --email your-email@example.com \
    -d api.yourdomain.com \
    --rsa-key-size 4096 \
    --agree-tos \
    --force-renewal" certbot
```

Then in `docker/nginx/conf.d/default.conf`, update the SSL certificate paths:
```nginx
ssl_certificate /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;
```

Reload Nginx:
```bash
docker compose exec nginx nginx -s reload
```
