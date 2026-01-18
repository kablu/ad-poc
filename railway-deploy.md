# Railway Deployment Guide for RA Web App

## Quick Deploy to Railway (Free Tier)

### Step 1: Prepare Your Spring Boot App

Your app is already configured for deployment. Just need to build:

```bash
# Build JAR file:
cd D:\ecc-dev\jdk-21-poc\ra-web
mvn clean package -DskipTests

# JAR will be at:
# target/ra-web-0.0.1-SNAPSHOT.jar
```

### Step 2: Create Railway Project

1. Visit: https://railway.app
2. Click "Start a New Project"
3. Choose "Deploy from GitHub repo"
4. Connect your GitHub account
5. Push your code to GitHub:

```bash
# Initialize git (if not already):
git init
git add .
git commit -m "Initial commit - RA Web App"

# Create GitHub repo and push:
git remote add origin https://github.com/yourusername/ra-web.git
git branch -M main
git push -u origin main
```

6. Select your repo in Railway
7. Railway will auto-detect Spring Boot and deploy!

### Step 3: Configure Environment

In Railway Dashboard:
- Go to Variables tab
- Add environment variables:
  ```
  SPRING_PROFILES_ACTIVE=prod
  SERVER_PORT=8080
  ```

### Step 4: Get Railway URL

Railway will give you a URL like:
```
https://ra-web-production.up.railway.app
```

### Step 5: Connect Custom Domain (GoDaddy)

1. **In Railway:**
   - Settings → Domains
   - Click "Add Domain"
   - Enter: yourdomain.com
   - Railway will show DNS records to add

2. **In GoDaddy:**
   - Domains → Manage DNS
   - Add CNAME record:
     ```
     Type: CNAME
     Name: @
     Value: ra-web-production.up.railway.app
     TTL: 600 seconds
     ```

3. **Wait for DNS propagation** (1-2 hours)

4. **Enable SSL (Automatic)**
   - Railway automatically provides free SSL certificate
   - Your site will be https://yourdomain.com

### Alternative: Use Subdomain

If root domain CNAME not working:
```
GoDaddy DNS:
Type: CNAME
Name: app (or www)
Value: ra-web-production.up.railway.app

Your site: https://app.yourdomain.com
```

### Step 6: Monitor Deployment

Railway Dashboard shows:
- Build logs
- Deployment status
- Resource usage
- Crash reports

### Free Tier Limits:
- 500 hours/month execution time
- 512MB RAM
- 1GB disk
- Custom domain: ✅ Included
- SSL: ✅ Automatic

### Cost After Free Tier:
- $5-10/month for basic app
- Pay only for what you use
