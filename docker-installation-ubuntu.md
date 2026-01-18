# Docker Installation Guide for Ubuntu
## Complete Setup for Docker Engine and Docker Compose

**Date**: 2026-01-15
**Supported Ubuntu Versions**: 20.04 LTS, 22.04 LTS, 24.04 LTS
**Purpose**: Install Docker for RA Web Application Development

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Installation Methods](#2-installation-methods)
3. [Method 1: Install Using apt Repository (Recommended)](#3-method-1-install-using-apt-repository-recommended)
4. [Method 2: Install from Package](#4-method-2-install-from-package)
5. [Method 3: Convenience Script](#5-method-3-convenience-script)
6. [Post-Installation Steps](#6-post-installation-steps)
7. [Install Docker Compose](#7-install-docker-compose)
8. [Verification](#8-verification)
9. [Troubleshooting](#9-troubleshooting)
10. [Uninstallation](#10-uninstallation)

---

## 1. Prerequisites

### 1.1 System Requirements

- **OS**: Ubuntu 20.04, 22.04, or 24.04 (64-bit)
- **Architecture**: x86_64/amd64, armhf, arm64, s390x
- **Kernel**: 3.10 or higher
- **Storage**: At least 10 GB free disk space
- **RAM**: Minimum 2 GB (4 GB recommended)

### 1.2 Check Your System

```bash
# Check Ubuntu version
lsb_release -a

# Check architecture
uname -m

# Check kernel version
uname -r

# Check disk space
df -h

# Check available memory
free -h
```

### 1.3 Remove Old Docker Versions (If Installed)

```bash
# Remove old versions
sudo apt-get remove docker docker-engine docker.io containerd runc

# It's OK if apt-get reports that none of these packages are installed
```

---

## 2. Installation Methods

There are three ways to install Docker:

1. **apt repository** (Recommended) - Easy updates, official support
2. **Package file** - Manual installation, for air-gapped systems
3. **Convenience script** - Quick setup for testing environments

---

## 3. Method 1: Install Using apt Repository (Recommended)

### 3.1 Update Package Index

```bash
# Update package list
sudo apt-get update

# Install prerequisites
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release
```

### 3.2 Add Docker's Official GPG Key

```bash
# Create directory for keyrings
sudo install -m 0755 -d /etc/apt/keyrings

# Download Docker's GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set permissions
sudo chmod a+r /etc/apt/keyrings/docker.gpg
```

### 3.3 Set Up Docker Repository

```bash
# Add Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

### 3.4 Install Docker Engine

```bash
# Update package index with Docker repository
sudo apt-get update

# Install Docker Engine, CLI, containerd, and plugins
sudo apt-get install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

# Verify installation
docker --version
```

**Expected Output**:
```
Docker version 24.0.7, build afdd53b
```

---

## 4. Method 2: Install from Package

### 4.1 Download Docker Package

**Visit**: https://download.docker.com/linux/ubuntu/dists/

**Select Your Version**:
1. Choose your Ubuntu version (e.g., `jammy` for 22.04)
2. Go to `pool/stable/`
3. Select your architecture (e.g., `amd64`)
4. Download `.deb` files for:
   - `containerd.io_<version>_<arch>.deb`
   - `docker-ce_<version>_<arch>.deb`
   - `docker-ce-cli_<version>_<arch>.deb`
   - `docker-buildx-plugin_<version>_<arch>.deb`
   - `docker-compose-plugin_<version>_<arch>.deb`

### 4.2 Install Packages

```bash
# Navigate to download directory
cd ~/Downloads

# Install all packages
sudo dpkg -i ./containerd.io_*.deb \
  ./docker-ce_*.deb \
  ./docker-ce-cli_*.deb \
  ./docker-buildx-plugin_*.deb \
  ./docker-compose-plugin_*.deb

# Fix dependencies if needed
sudo apt-get install -f

# Verify installation
docker --version
```

---

## 5. Method 3: Convenience Script

**⚠️ Warning**: Only use this for testing environments, not production!

### 5.1 Run Installation Script

```bash
# Download and run Docker installation script
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Clean up
rm get-docker.sh

# Verify installation
docker --version
```

### 5.2 Specify Version (Optional)

```bash
# Install specific version
sudo sh get-docker.sh --version 24.0.7
```

---

## 6. Post-Installation Steps

### 6.1 Start Docker Service

```bash
# Start Docker daemon
sudo systemctl start docker

# Enable Docker to start on boot
sudo systemctl enable docker

# Check Docker service status
sudo systemctl status docker
```

**Expected Output**:
```
● docker.service - Docker Application Container Engine
     Loaded: loaded (/lib/systemd/system/docker.service; enabled; vendor preset: enabled)
     Active: active (running) since Wed 2026-01-15 10:30:00 UTC; 5min ago
```

### 6.2 Add User to Docker Group (Run Docker Without sudo)

```bash
# Add your user to docker group
sudo usermod -aG docker $USER

# Apply group changes (logout/login or run):
newgrp docker

# Verify group membership
	groups

# Test without sudo
docker run hello-world
```

**Expected Output**:
```
Hello from Docker!
This message shows that your installation appears to be working correctly.
```

### 6.3 Configure Docker Daemon

**Create daemon configuration file**:

```bash
# Create or edit Docker daemon config
sudo nano /etc/docker/daemon.json
```

**Add configuration**:

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "dns": ["8.8.8.8", "8.8.4.4"],
  "default-address-pools": [
    {
      "base": "172.17.0.0/16",
      "size": 24
    }
  ]
}
```

**Restart Docker**:

```bash
# Restart Docker service
sudo systemctl restart docker

# Verify configuration
docker info | grep -i storage
docker info | grep -i log
```

---

## 7. Install Docker Compose

### 7.1 Docker Compose Plugin (Installed with Docker Engine)

If you installed Docker using Method 1, Docker Compose plugin is already installed.

```bash
# Verify Docker Compose
docker compose version
```

**Expected Output**:
```
Docker Compose version v2.23.3
```

### 7.2 Install Docker Compose Standalone (Alternative)

**If you need standalone Docker Compose v2**:

```bash
# Download Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.23.3/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose

# Make executable
sudo chmod +x /usr/local/bin/docker-compose

# Create symlink (optional)
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# Verify installation
docker-compose --version
```

**Expected Output**:
```
Docker Compose version v2.23.3
```

### 7.3 Docker Compose v1 (Legacy - Not Recommended)

**Only if you need v1 for legacy compatibility**:

```bash
sudo apt-get install docker-compose

# Verify
docker-compose --version
```

---

## 8. Verification

### 8.1 Test Docker Installation

```bash
# 1. Check Docker version
docker --version

# 2. Check Docker Compose version
docker compose version

# 3. Display Docker system information
docker info

# 4. Run hello-world container
docker run hello-world

# 5. List running containers
docker ps

# 6. List all containers (including stopped)
docker ps -a

# 7. List Docker images
docker images

# 8. Check Docker service status
sudo systemctl status docker
```

### 8.2 Run a Test Container

```bash
# Pull Ubuntu image
docker pull ubuntu:22.04

# Run interactive Ubuntu container
docker run -it ubuntu:22.04 bash

# Inside container, run:
cat /etc/os-release
exit

# Clean up test container
docker rm $(docker ps -a -q -f ancestor=ubuntu:22.04)
docker rmi ubuntu:22.04
```

### 8.3 Test Docker Compose

**Create test docker-compose.yml**:

```bash
# Create test directory
mkdir ~/docker-test
cd ~/docker-test

# Create docker-compose.yml
cat > docker-compose.yml <<EOF
version: '3.8'

services:
  web:
    image: nginx:alpine
    ports:
      - "8080:80"
    environment:
      - NGINX_HOST=localhost
      - NGINX_PORT=80
EOF

# Start services
docker compose up -d

# Check running containers
docker compose ps

# Test web server
curl http://localhost:8080

# View logs
docker compose logs web

# Stop services
docker compose down

# Clean up
cd ~
rm -rf ~/docker-test
```

---

## 9. Troubleshooting

### 9.1 Permission Denied Error

**Error**: `Got permission denied while trying to connect to the Docker daemon socket`

**Solution**:
```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Logout and login again, or run:
newgrp docker

# Verify
docker run hello-world
```

### 9.2 Docker Daemon Not Starting

**Error**: `Cannot connect to the Docker daemon`

**Solution**:
```bash
# Check Docker service status
sudo systemctl status docker

# If not running, start it
sudo systemctl start docker

# Enable on boot
sudo systemctl enable docker

# Check logs
sudo journalctl -u docker.service -n 50 --no-pager

# Restart Docker
sudo systemctl restart docker
```

### 9.3 Storage Driver Issues

**Error**: `Error starting daemon: error initializing graphdriver`

**Solution**:
```bash
# Check current storage driver
docker info | grep "Storage Driver"

# If using aufs and having issues, switch to overlay2
sudo nano /etc/docker/daemon.json

# Add:
{
  "storage-driver": "overlay2"
}

# Restart Docker
sudo systemctl restart docker
```

### 9.4 Network Issues

**Error**: `network not found` or containers can't reach internet

**Solution**:
```bash
# Restart Docker network
sudo systemctl restart docker

# Check network list
docker network ls

# Inspect default bridge network
docker network inspect bridge

# Reset Docker network (WARNING: stops all containers)
sudo systemctl stop docker
sudo rm -rf /var/lib/docker/network
sudo systemctl start docker
```

### 9.5 Disk Space Issues

**Error**: `no space left on device`

**Solution**:
```bash
# Check Docker disk usage
docker system df

# Remove unused containers
docker container prune -f

# Remove unused images
docker image prune -a -f

# Remove unused volumes
docker volume prune -f

# Remove all unused data
docker system prune -a -f --volumes

# Check space again
docker system df
```

### 9.6 DNS Resolution Issues

**Error**: Containers can't resolve domain names

**Solution**:
```bash
# Edit Docker daemon config
sudo nano /etc/docker/daemon.json

# Add DNS servers:
{
  "dns": ["8.8.8.8", "8.8.4.4", "1.1.1.1"]
}

# Restart Docker
sudo systemctl restart docker

# Test in container
docker run --rm alpine nslookup google.com
```

### 9.7 Port Already in Use

**Error**: `bind: address already in use`

**Solution**:
```bash
# Find process using port (e.g., 8080)
sudo lsof -i :8080

# Or use netstat
sudo netstat -tlnp | grep 8080

# Kill process (replace PID)
sudo kill -9 <PID>

# Or use different port in docker-compose.yml
ports:
  - "8081:80"
```

---

## 10. Uninstallation

### 10.1 Remove Docker Engine

```bash
# Uninstall Docker packages
sudo apt-get purge -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

# Remove dependencies
sudo apt-get autoremove -y

# Remove Docker repository
sudo rm /etc/apt/sources.list.d/docker.list
sudo rm /etc/apt/keyrings/docker.gpg
```

### 10.2 Delete All Docker Data (Optional)

**⚠️ Warning**: This deletes all images, containers, volumes, and networks!

```bash
# Delete all Docker data
sudo rm -rf /var/lib/docker
sudo rm -rf /var/lib/containerd

# Delete Docker config
sudo rm -rf /etc/docker

# Remove Docker group
sudo groupdel docker
```

---

## 11. Additional Configuration

### 11.1 Enable IPv6 Support

```bash
# Edit daemon.json
sudo nano /etc/docker/daemon.json

# Add:
{
  "ipv6": true,
  "fixed-cidr-v6": "2001:db8:1::/64"
}

# Restart Docker
sudo systemctl restart docker
```

### 11.2 Configure Resource Limits

```bash
# Edit daemon.json
sudo nano /etc/docker/daemon.json

# Add:
{
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 64000,
      "Soft": 64000
    }
  }
}

# Restart Docker
sudo systemctl restart docker
```

### 11.3 Enable Docker Metrics

```bash
# Edit daemon.json
sudo nano /etc/docker/daemon.json

# Add:
{
  "metrics-addr": "0.0.0.0:9323",
  "experimental": true
}

# Restart Docker
sudo systemctl restart docker

# Access metrics
curl http://localhost:9323/metrics
```

---

## 12. Quick Reference

### 12.1 Essential Commands

```bash
# System
docker --version              # Show Docker version
docker info                   # Display system-wide information
docker system df              # Show disk usage
docker system prune -a        # Clean up unused resources

# Images
docker images                 # List images
docker pull <image>           # Pull image from registry
docker rmi <image>            # Remove image
docker build -t <name> .      # Build image from Dockerfile

# Containers
docker ps                     # List running containers
docker ps -a                  # List all containers
docker run <image>            # Run container
docker stop <container>       # Stop container
docker rm <container>         # Remove container
docker logs <container>       # View logs
docker exec -it <container> bash  # Execute command in container

# Docker Compose
docker compose up -d          # Start services in background
docker compose down           # Stop and remove services
docker compose ps             # List services
docker compose logs           # View logs
docker compose restart        # Restart services
```

### 12.2 Useful Aliases

Add to `~/.bashrc` or `~/.zshrc`:

```bash
# Docker aliases
alias d='docker'
alias dc='docker compose'
alias dps='docker ps'
alias dpsa='docker ps -a'
alias di='docker images'
alias dcu='docker compose up -d'
alias dcd='docker compose down'
alias dcl='docker compose logs -f'
alias dclean='docker system prune -a -f'
```

Apply aliases:
```bash
source ~/.bashrc
```

---

## 13. Security Best Practices

### 13.1 Run Docker as Non-Root User

Already covered in section 6.2 (add user to docker group).

### 13.2 Use Docker Content Trust

```bash
# Enable content trust
export DOCKER_CONTENT_TRUST=1

# Pull signed images only
docker pull alpine
```

### 13.3 Scan Images for Vulnerabilities

```bash
# Install Docker Scout (if not already installed)
docker scout quickview <image>

# Detailed vulnerability report
docker scout cves <image>
```

### 13.4 Limit Container Resources

```bash
# Limit CPU and memory
docker run -d \
  --cpus="1.5" \
  --memory="512m" \
  --memory-swap="1g" \
  nginx:alpine
```

---

## 14. Next Steps

After successful installation:

1. ✅ **Start Samba AD**: Run the Samba Active Directory docker-compose file
   ```bash
   cd D:\ecc-dev\jdk-21-poc\ra-web
   docker compose -f docker-compose-samba-ad.yml up -d
   ```

2. ✅ **Learn Docker Basics**: Complete Docker tutorial
   ```bash
   docker run -d -p 80:80 docker/getting-started
   # Visit: http://localhost
   ```

3. ✅ **Configure Development Environment**: Set up your IDE Docker integration

4. ✅ **Read Documentation**: https://docs.docker.com/

---

## 15. Resources

- **Official Docs**: https://docs.docker.com/engine/install/ubuntu/
- **Docker Hub**: https://hub.docker.com/
- **Docker Compose Docs**: https://docs.docker.com/compose/
- **Best Practices**: https://docs.docker.com/develop/dev-best-practices/
- **Security**: https://docs.docker.com/engine/security/

---

**Document End**

**Prepared By**: DevOps Team
**Last Updated**: 2026-01-15
**Status**: Production Ready
