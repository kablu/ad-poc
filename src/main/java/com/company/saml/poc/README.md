# SAML POC - Proof of Concept Implementation

## Overview

This package contains a complete SAML 2.0 Service Provider (SP) implementation for the RA Web Application. It demonstrates Single Sign-On (SSO) authentication using SAML assertions from an Identity Provider (IdP).

## Package Structure

```
com.company.saml.poc/
├── SAMLPocApplication.java          # Main Spring Boot application
├── config/
│   └── SAMLSecurityConfig.java      # SAML security configuration
├── controller/
│   ├── HomeController.java          # Public pages (home, login)
│   ├── DashboardController.java     # Role-based dashboards
│   └── SAMLController.java          # SAML-specific endpoints
├── model/
│   └── SAMLUser.java                # User model from SAML assertion
├── service/
│   └── SAMLUserService.java         # User service layer
├── validator/
│   └── SAMLAssertionValidator.java  # SAML assertion validation
└── util/
    └── SAMLMetadataGenerator.java   # SP metadata generation
```

## Features

### 1. SAML Authentication
- **SP-Initiated SSO**: User starts at SP, redirected to IdP for authentication
- **IdP-Initiated SSO**: User starts at IdP portal, clicks app link
- **Single Logout (SLO)**: Logout from SP triggers IdP logout

### 2. User Attribute Extraction
Extracts the following attributes from SAML assertions:
- Email address
- First name and last name
- Display name
- Department
- Roles (for RBAC)

### 3. Role-Based Access Control (RBAC)
- **RA_ADMIN**: Full administrative access
- **RA_OFFICER**: Certificate approval/revocation
- **RA_OPERATOR**: Certificate request submission
- **AUDITOR**: Read-only access to audit logs
- **END_ENTITY**: Self-service certificate requests

### 4. Security Features
- Digital signature verification on SAML assertions
- Assertion replay attack prevention
- Timestamp validation (NotBefore/NotOnOrAfter)
- Audience restriction validation
- Issuer verification
- Subject confirmation validation

## Setup Instructions

### Step 1: Generate SAML Keystore

```bash
# Navigate to resources directory
cd src/main/resources

# Generate keystore with self-signed certificate
keytool -genkeypair \
    -alias saml-signing \
    -keyalg RSA \
    -keysize 2048 \
    -keystore saml-keystore.jks \
    -storepass changeit \
    -keypass changeit \
    -dname "CN=localhost, OU=IT, O=Company, L=City, ST=State, C=US" \
    -validity 3650

# Export certificate for IdP
keytool -export \
    -alias saml-signing \
    -file saml-sp-certificate.cer \
    -keystore saml-keystore.jks \
    -storepass changeit
```

### Step 2: Configure Identity Provider

1. **Get SP Metadata**
   - Start the application
   - Access: `https://localhost:8443/saml2/service-provider-metadata/saml-poc`
   - Save the XML metadata

2. **Upload to IdP**
   - Login to your IdP admin console
   - Register new Service Provider
   - Upload SP metadata XML
   - Upload SP certificate (`saml-sp-certificate.cer`)

3. **Configure Attribute Mapping** (at IdP)
   - Map user attributes to SAML claims:
     ```
     User Attribute  →  SAML Attribute Name
     ─────────────────────────────────────────
     email           →  email
     firstName       →  firstName
     lastName        →  lastName
     displayName     →  displayName
     department      →  department
     groups/roles    →  role
     ```

4. **Get IdP Metadata**
   - Download IdP metadata XML
   - Note the IdP metadata URL

### Step 3: Configure Application

Edit `src/main/resources/application-saml.yml`:

```yaml
saml:
  idp:
    entity-id: https://your-idp.example.com
    metadata-url: https://your-idp.example.com/metadata

  sp:
    entity-id: https://localhost:8443/saml/metadata
    acs-url: https://localhost:8443/login/saml2/sso/saml-poc
```

### Step 4: Run Application

```bash
# Run with SAML profile
mvn spring-boot:run -Dspring-boot.run.profiles=saml

# Or using Gradle
./gradlew bootRun --args='--spring.profiles.active=saml'
```

### Step 5: Test SAML Authentication

1. **Access Protected Resource**
   ```
   https://localhost:8443/dashboard
   ```

2. **Redirected to IdP Login**
   - Enter your IdP credentials
   - Authenticate (may include MFA)

3. **Returned to SP**
   - SAML assertion processed
   - User authenticated
   - Redirected to role-based dashboard

4. **View User Info**
   ```
   https://localhost:8443/saml/user-info
   ```

5. **Check SAML Attributes**
   ```
   https://localhost:8443/saml/attributes
   ```

6. **API Endpoint**
   ```
   curl -k https://localhost:8443/saml/api/user-info \
     -H "Cookie: JSESSIONID=your-session-id"
   ```

## Testing with SimpleSAMLphp (Local IdP)

For local testing without external IdP:

### 1. Install SimpleSAMLphp

```bash
# Using Docker
docker run -d --name simplesamlphp \
  -p 8080:8080 \
  -e SIMPLESAMLPHP_SP_ENTITY_ID=https://localhost:8443/saml/metadata \
  -e SIMPLESAMLPHP_SP_ASSERTION_CONSUMER_SERVICE=https://localhost:8443/login/saml2/sso/saml-poc \
  kristophjunge/test-saml-idp
```

### 2. Update Configuration

```yaml
saml:
  idp:
    metadata-url: http://localhost:8080/simplesaml/saml2/idp/metadata.php
```

### 3. Default Test Users

SimpleSAMLphp provides test users:
- Username: `user1` / Password: `user1pass`
- Username: `user2` / Password: `user2pass`

## API Endpoints

| Endpoint | Description | Auth Required |
|----------|-------------|---------------|
| `GET /` | Home page | No |
| `GET /login` | Login page | No |
| `GET /dashboard` | Main dashboard (redirects by role) | Yes |
| `GET /saml/user-info` | Display user info from SAML | Yes |
| `GET /saml/api/user-info` | User info as JSON | Yes |
| `GET /saml/attributes` | All SAML attributes | Yes |
| `GET /saml/status` | Authentication status | Yes |
| `GET /saml2/service-provider-metadata/saml-poc` | SP metadata XML | No |
| `POST /logout` | Logout (triggers SLO) | Yes |

## Role-Based Dashboards

| Role | Dashboard URL | Access Level |
|------|---------------|--------------|
| RA_ADMIN | `/admin/dashboard` | Full system access |
| RA_OFFICER | `/officer/dashboard` | Certificate approval/revocation |
| RA_OPERATOR | `/operator/dashboard` | Certificate request submission |
| AUDITOR | `/auditor/dashboard` | Read-only audit access |
| END_ENTITY | `/user/dashboard` | Self-service certificates |

## Security Validations

The SAML implementation performs the following security checks:

### 1. Signature Verification
- Verifies digital signature on SAML Response
- Validates against IdP's public certificate

### 2. Assertion Replay Prevention
- Tracks used assertion IDs
- Rejects duplicate assertions

### 3. Timestamp Validation
- `NotBefore`: Assertion not yet valid
- `NotOnOrAfter`: Assertion expired

### 4. Audience Restriction
- Ensures assertion intended for this SP
- Validates against SP entity ID

### 5. Issuer Verification
- Confirms assertion from trusted IdP
- Validates against expected IdP entity ID

### 6. Subject Confirmation
- Validates bearer confirmation method
- Checks recipient matches ACS URL

## Troubleshooting

### Issue: "Invalid signature"
**Solution:**
- Verify IdP certificate is trusted
- Check certificate hasn't expired
- Ensure SP metadata uploaded to IdP correctly

### Issue: "Assertion expired"
**Solution:**
- Check server time synchronization (NTP)
- Adjust clock skew tolerance in configuration
- Verify assertion validity period at IdP

### Issue: "Unknown issuer"
**Solution:**
- Verify `saml.idp.entity-id` matches IdP metadata
- Check IdP metadata URL is accessible
- Ensure IdP metadata hasn't changed

### Issue: "Invalid audience"
**Solution:**
- Verify `saml.sp.entity-id` in configuration
- Ensure SP metadata uploaded to IdP correctly
- Check IdP audience restriction configuration

### Issue: SSL certificate errors
**Solution:**
```bash
# Trust localhost certificate (development only)
# Add -Djavax.net.ssl.trustStore=... to JVM args
# Or import certificate to Java truststore
```

## Production Considerations

### 1. Certificate Management
- Use proper SSL/TLS certificates (not self-signed)
- Rotate SAML signing certificates regularly
- Store private keys securely (HSM recommended)

### 2. Assertion Replay Cache
- Use Redis or distributed cache
- Set TTL based on assertion validity
- Monitor cache size and performance

### 3. Session Management
- Configure appropriate session timeout
- Implement session fixation protection
- Use secure session cookies (HttpOnly, Secure, SameSite)

### 4. Logging and Monitoring
- Log all authentication events
- Monitor failed authentication attempts
- Set up alerts for security anomalies

### 5. High Availability
- Deploy multiple SP instances
- Use load balancer with sticky sessions
- Centralize session storage (Redis, database)

### 6. Metadata Updates
- Monitor IdP metadata for changes
- Automate metadata refresh process
- Validate new metadata before applying

## Integration with RA Web Application

This SAML POC can be integrated into the main RA Web Application:

### 1. Replace AD Direct Authentication
```java
// Before: Direct AD authentication
@Autowired
private ActiveDirectoryService adService;

// After: SAML authentication
@Autowired
private SAMLUserService samlUserService;
```

### 2. Extract User Attributes
```java
SAMLUser user = samlUserService.getCurrentUser();
String email = user.getEmail();
String department = user.getDepartment();
List<String> roles = user.getRoles();
```

### 3. Role-Based Access Control
```java
if (samlUserService.hasRole("RA_OFFICER")) {
    // Allow certificate approval
}
```

### 4. Certificate Request Submission
```java
// User authenticated via SAML
SAMLUser user = samlUserService.getCurrentUser();

// Validate CSR subject DN against SAML attributes
String csrEmail = extractEmailFromCSR(csr);
if (!csrEmail.equals(user.getEmail())) {
    throw new ValidationException("CSR email doesn't match authenticated user");
}
```

## Further Reading

- [SAML 2.0 Specification](https://docs.oasis-open.org/security/saml/v2.0/)
- [Spring Security SAML Documentation](https://docs.spring.io/spring-security/reference/servlet/saml2/index.html)
- [OWASP SAML Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html)

## License

This SAML POC is part of the RA Web Application project.

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review application logs: `logs/saml-poc.log`
3. Enable DEBUG logging for detailed SAML flow
4. Contact SAML POC Team
