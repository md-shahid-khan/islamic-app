@echo off
setlocal

set DOCKER_USER=%1
if "%DOCKER_USER%"=="" (
    set /p DOCKER_USER="Enter your Docker Hub username: "
)

if "%DOCKER_USER%"=="" (
    echo [ERROR] Docker Hub username is required.
    exit /b 1
)

echo.
echo [1/3] Tagging image as %DOCKER_USER%/angusmasail-backend:latest ...
docker tag angusmasail-backend:latest %DOCKER_USER%/angusmasail-backend:latest
if %errorlevel% neq 0 (
    echo [ERROR] Failed to tag image. Make sure angusmasail-backend:latest is built.
    exit /b %errorlevel%
)

echo.
echo [2/3] Logging in to Docker Hub ...
docker login
if %errorlevel% neq 0 (
    echo [ERROR] Docker login failed.
    exit /b %errorlevel%
)

echo.
echo [3/3] Pushing %DOCKER_USER%/angusmasail-backend:latest to Docker Hub ...
docker push %DOCKER_USER%/angusmasail-backend:latest
if %errorlevel% neq 0 (
    echo [ERROR] Docker push failed.
    exit /b %errorlevel%
)

echo.
echo ==============================================================================
echo [SUCCESS] Image pushed successfully!
echo Repository: %DOCKER_USER%/angusmasail-backend:latest
echo On your AWS EC2 instance, set:
echo export DOCKER_IMAGE=%DOCKER_USER%/angusmasail-backend:latest
echo docker compose up -d
echo ==============================================================================
