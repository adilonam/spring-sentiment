# GitHub Actions Setup for Spring Sentiment Gateway

This repository contains GitHub Actions workflows for CI/CD of the Spring Boot Gateway application.

## Workflows

### 1. Main Deployment Workflow (`docker-build-deploy.yml`)
- **Triggers**: Push to `main`/`master` branch
- **Jobs**:
  - **Test**: Runs unit tests with JUnit reporting
  - **Build & Push**: Builds Docker image and pushes to Docker Hub
  - **Deploy**: Deploys to production VM via SSH
  - **Security Scan**: Scans Docker image for vulnerabilities

### 2. CI Workflow (`ci.yml`)
- **Triggers**: Push to feature branches, PRs to main
- **Jobs**:
  - **Test**: Runs unit tests
  - **Build**: Builds application and Docker image (no push)

### 3. Dependency Updates (`dependency-updates.yml`)
- **Triggers**: Weekly schedule (Mondays) or manual
- **Jobs**: Updates Maven dependencies and creates PR

## Required Secrets

Set these secrets in your GitHub repository settings (`Settings` → `Secrets and variables` → `Actions`):

### Docker Hub
```
DOCKERHUB_USERNAME=your-dockerhub-username
DOCKERHUB_TOKEN=your-dockerhub-access-token
```

### VM Deployment
```
VM_HOST=your.server.ip.address
VM_USERNAME=your-ssh-username
VM_SSH_KEY=your-private-ssh-key
VM_SSH_PORT=22  # Optional, defaults to 22
```

### Application Configuration
```
DB_PASSWORD=your-database-password
FASTAPI_URL=http://your-fastapi-service:8000
KEYCLOAK_URL=http://your-keycloak:8080
KEYCLOAK_REALM=your-realm
KEYCLOAK_CLIENT_ID=your-client-id
KEYCLOAK_CLIENT_SECRET=your-client-secret
REDIS_HOST=your-redis-host
REDIS_PORT=6379
```

## Setup Instructions

### 1. Docker Hub Setup
1. Create a Docker Hub account if you don't have one
2. Create an access token: Docker Hub → Account Settings → Security → New Access Token
3. Add `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` to GitHub secrets

### 2. Server Setup
1. Ensure Docker is installed on your target server
2. Generate SSH key pair for GitHub Actions:
   ```bash
   ssh-keygen -t ed25519 -C "github-actions"
   ```
3. Add the public key to your server's `~/.ssh/authorized_keys`
4. Add the private key content to GitHub secret `VM_SSH_KEY`
5. Add server details to GitHub secrets (`VM_HOST`, `VM_USERNAME`, `VM_SSH_PORT`)

### 3. Environment Configuration
1. Update the environment variables in the deployment script as needed
2. Ensure your server has the required services (PostgreSQL, Redis, Keycloak) running
3. The deployment script creates a default `.env` file, but you may want to customize it

### 4. Security Considerations
- The workflow uses a non-root user in the Docker container
- Implements health checks for the deployed container
- Includes vulnerability scanning with Trivy
- Uses environment protection for production deployments

## Customization

### Changing the Image Name
Update the `IMAGE_NAME` environment variable in the workflow files:
```yaml
env:
  IMAGE_NAME: your-custom-image-name
```

### Adding Environment Protection
1. Go to repository Settings → Environments
2. Create a "production" environment
3. Add protection rules (required reviewers, deployment branches, etc.)

### Custom Deployment Script
Modify the deployment section in `docker-build-deploy.yml` to match your server setup:
- Change ports if your app runs on a different port
- Modify volume mounts for logs or data persistence
- Update health check endpoints if different

## Monitoring Deployment

After deployment, you can monitor your application:
- **Health Check**: `http://your-server:8080/actuator/health`
- **Application Logs**: `docker logs spring-gateway-container`
- **Container Status**: `docker ps --filter name=spring-gateway-container`

## Troubleshooting

### Common Issues

1. **Build Fails**: Check Java version compatibility (requires JDK 21)
2. **Docker Push Fails**: Verify Docker Hub credentials
3. **Deployment Fails**: Check SSH connectivity and server Docker installation
4. **Container Won't Start**: Review application logs and environment variables

### Debug Commands

```bash
# Check container logs
docker logs spring-gateway-container

# Inspect container
docker inspect spring-gateway-container

# Check environment variables
docker exec spring-gateway-container env

# Test application endpoints
curl http://localhost:8080/actuator/health
```
