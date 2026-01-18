# SAML POC - Quick Start Guide

## Overview
This guide will help you quickly set up and run the SAML POC application.

---

## Prerequisites

- Java 21 or higher
- Maven 3.8+ or Gradle 8+
- Access to an Identity Provider (IdP) OR use local SimpleSAMLphp for testing

---

## Quick Setup (5 Minutes)

### Step 1: Generate SAML Keystore (1 minute)

```bash
cd src/main/resources

keytool -genkeypair \
  -alias saml-signing \
  -keyalg RSA \
  -keysize 2048 \
  -keystore saml-keystore.jks \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=localhost, OU=IT, O=Company, C=US" \
  -validity 3650
```

### Step 2: Configure Identity Provider (2 minutes)

Edit `src/main/resources/application-saml.yml`:

```yaml
saml:
  idp:
    metadata-url: https://your-idp.example.com/metadata
    # OR for testing with SimpleSAMLphp:
    # metadata-url: http://localhost:8080/simplesaml/saml2/idp/metadata.php
```

### Step 3: Run Application (1 minute)

```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=saml

# OR using Gradle
./gradlew bootRun --args='--spring.profiles.active=saml'
```

### Step 4: Test Authentication (1 minute)

1. Open browser: `https://localhost:8443`
2. Click "Login with SAML"
3. Authenticate at IdP
4. You should be redirected back and see the dashboard!

---

## Testing with SimpleSAMLphp (Local IdP)

### Option A: Using Docker (Easiest)

```bash
# Start SimpleSAMLphp IdP
docker run -d --name saml-idp \
  -p 8080:8080 \
  -e SIMPLESAMLPHP_SP_ENTITY_ID=https://localhost:8443/saml/metadata \
  -e SIMPLESAMLPHP_SP_ASSERTION_CONSUMER_SERVICE=https://localhost:8443/login/saml2/sso/saml-poc \
  kristophjunge/test-saml-idp

# Update application-saml.yml
saml:
  idp:
    metadata-url: http://localhost:8080/simplesaml/saml2/idp/metadata.php
```

### Test Users (SimpleSAMLphp)
- Username: `user1` / Password: `user1pass`
- Username: `user2` / Password: `user2pass`

---

## Application Endpoints

| URL | Description |
|-----|-------------|
| `https://localhost:8443` | Home page |
| `https://localhost:8443/login` | Login page |
| `https://localhost:8443/dashboard` | Dashboard (requires auth) |
| `https://localhost:8443/saml/user-info` | SAML user info |
| `https://localhost:8443/saml/api/user-info` | User info JSON API |
| `https://localhost:8443/saml2/service-provider-metadata/saml-poc` | SP Metadata |

---

## Verify SAML Flow

### 1. Check Application Started
```bash
# Look for this in console:
==============================================
SAML POC Application Started Successfully!
==============================================
Access the application at: https://localhost:8443
SAML Metadata: https://localhost:8443/saml/metadata
==============================================
```

### 2. Get SP Metadata
```bash
curl -k https://localhost:8443/saml2/service-provider-metadata/saml-poc
```

Should return XML like:
```xml
<?xml version="1.0"?>
<EntityDescriptor entityID="https://localhost:8443/saml/metadata" ...>
  <SPSSODescriptor ...>
    ...
  </SPSSODescriptor>
</EntityDescriptor>
```

### 3. Test Authentication
```bash
# Access protected resource (will redirect to IdP)
curl -k -L https://localhost:8443/dashboard

# Should redirect to IdP login page
```

### 4. Check Logs
```bash
# Look for SAML debug logs
tail -f logs/saml-poc.log
```

---

## Role-Based Testing

### Test Different Roles

1. **RA Admin**
   - Configure IdP to return `role: RA_ADMIN`
   - Login → Should redirect to `/admin/dashboard`

2. **RA Officer**
   - Configure IdP to return `role: RA_OFFICER`
   - Login → Should redirect to `/officer/dashboard`

3. **RA Operator**
   - Configure IdP to return `role: RA_OPERATOR`
   - Login → Should redirect to `/operator/dashboard`

4. **End Entity** (default)
   - No role or unknown role
   - Login → Should redirect to `/user/dashboard`

---

## Troubleshooting

### Issue 1: "Connection refused to localhost:8443"
**Solution:**
```bash
# Check if application is running
netstat -an | grep 8443

# Restart application
mvn spring-boot:run -Dspring-boot.run.profiles=saml
```

### Issue 2: "Invalid signature on SAML response"
**Solution:**
```bash
# Regenerate keystore
rm src/main/resources/saml-keystore.jks
# Run Step 1 again to generate keystore
# Upload new SP certificate to IdP
```

### Issue 3: "IdP metadata not found"
**Solution:**
```yaml
# Verify IdP metadata URL is accessible
curl http://localhost:8080/simplesaml/saml2/idp/metadata.php

# Update configuration
saml:
  idp:
    metadata-url: http://localhost:8080/simplesaml/saml2/idp/metadata.php
```

### Issue 4: Browser shows "Your connection is not private"
**Solution:**
```
# For development/testing only, click "Advanced" → "Proceed to localhost (unsafe)"
# In production, use proper SSL certificate
```

### Issue 5: "Assertion expired"
**Solution:**
```bash
# Synchronize server time with NTP
sudo ntpdate -s time.nist.gov

# OR adjust clock skew tolerance in IdP configuration
```

---

## Testing Checklist

- [ ] Application starts without errors
- [ ] SP metadata accessible at `/saml2/service-provider-metadata/saml-poc`
- [ ] Accessing `/dashboard` redirects to IdP
- [ ] Can login at IdP with test credentials
- [ ] After login, redirected back to SP with user info
- [ ] User attributes displayed on `/saml/user-info`
- [ ] Role-based dashboard routing works
- [ ] Logout works and terminates session

---

## Next Steps

1. **Configure Production IdP**
   - Replace SimpleSAMLphp with your organization's IdP (Okta, Azure AD, ADFS)
   - Upload SP metadata to production IdP
   - Update `application-saml.yml` with production IdP metadata URL

2. **Attribute Mapping**
   - Map AD groups to SAML roles at IdP
   - Configure attribute names to match your IdP

3. **SSL Certificates**
   - Generate proper SSL certificate for production
   - Update keystore configuration

4. **Integration**
   - Integrate SAML authentication with RA Web Application
   - Replace AD direct authentication with SAML
   - Use SAMLUserService for user information

5. **Production Deployment**
   - Deploy to production server
   - Configure load balancer
   - Set up monitoring and logging

---

## Sample SAML Assertion

After successful authentication, SAML assertion should contain:

```xml
<saml:Assertion>
  <saml:Subject>
    <saml:NameID>user@example.com</saml:NameID>
  </saml:Subject>
  <saml:AttributeStatement>
    <saml:Attribute Name="email">
      <saml:AttributeValue>user@example.com</saml:AttributeValue>
    </saml:Attribute>
    <saml:Attribute Name="firstName">
      <saml:AttributeValue>John</saml:AttributeValue>
    </saml:Attribute>
    <saml:Attribute Name="lastName">
      <saml:AttributeValue>Doe</saml:AttributeValue>
    </saml:Attribute>
    <saml:Attribute Name="department">
      <saml:AttributeValue>Engineering</saml:AttributeValue>
    </saml:Attribute>
    <saml:Attribute Name="role">
      <saml:AttributeValue>RA_OFFICER</saml:AttributeValue>
    </saml:Attribute>
  </saml:AttributeStatement>
</saml:Assertion>
```

---

## Quick Reference Commands

```bash
# Start application
mvn spring-boot:run -Dspring-boot.run.profiles=saml

# View logs
tail -f logs/saml-poc.log

# Test SP metadata
curl -k https://localhost:8443/saml2/service-provider-metadata/saml-poc

# Test user info API (after login)
curl -k https://localhost:8443/saml/api/user-info \
  -H "Cookie: JSESSIONID=your-session-id"

# Generate new keystore
keytool -genkeypair -alias saml-signing \
  -keyalg RSA -keysize 2048 \
  -keystore saml-keystore.jks \
  -storepass changeit

# Export certificate
keytool -export -alias saml-signing \
  -file sp-certificate.cer \
  -keystore saml-keystore.jks

# Start SimpleSAMLphp (Docker)
docker run -d --name saml-idp -p 8080:8080 \
  kristophjunge/test-saml-idp

# Stop SimpleSAMLphp
docker stop saml-idp
docker rm saml-idp
```

---

## Support

For detailed information, see:
- Full README: `src/main/java/com/company/saml/poc/README.md`
- SAML Overview: `saml/SAML_Overview.md`
- SAML Steps: `saml/SAML_Steps_Detailed.md`
- Java Examples: `saml/SAML_Java_Example.md`

Happy SAML testing! 🚀
