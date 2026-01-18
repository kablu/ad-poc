# SAML Compilation Error Fix

## Problem

The SAML POC classes in package `com.company.saml.poc` had compilation errors because the required Spring Security SAML2 and OpenSAML dependencies were missing from the Maven `pom.xml` file.

## Symptoms

- Compilation errors in SAML-related classes:
  - `SAMLSecurityConfig.java`
  - `SAMLAssertionValidator.java`
  - Other SAML POC classes

- Missing imports:
  - `org.springframework.security.saml2.*`
  - `org.opensaml.saml.*`

## Root Cause

The `pom.xml` file was missing:
1. Spring Security SAML2 Service Provider dependency
2. OpenSAML core and SAML implementation dependencies
3. Thymeleaf template engine (required for SAML views)
4. Shibboleth Maven repository (OpenSAML artifacts are not in Maven Central)

## Solution Applied

### 1. Added Shibboleth Repository

OpenSAML artifacts are hosted in the Shibboleth repository, not Maven Central.

```xml
<repositories>
    <!-- Shibboleth Repository for OpenSAML -->
    <repository>
        <id>shibboleth</id>
        <name>Shibboleth Repository</name>
        <url>https://build.shibboleth.net/maven/releases/</url>
    </repository>
</repositories>
```

### 2. Added OpenSAML Version Property

```xml
<properties>
    ...
    <opensaml.version>5.1.2</opensaml.version>
</properties>
```

### 3. Added Spring Security SAML2 Dependency

```xml
<!-- Spring Security SAML2 Service Provider -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-saml2-service-provider</artifactId>
</dependency>
```

This dependency is managed by Spring Boot parent POM (version 4.0.1), so no explicit version is needed.

### 4. Added OpenSAML Dependencies

```xml
<!-- OpenSAML Core API -->
<dependency>
    <groupId>org.opensaml</groupId>
    <artifactId>opensaml-core-api</artifactId>
    <version>${opensaml.version}</version>
</dependency>

<!-- OpenSAML Core Implementation -->
<dependency>
    <groupId>org.opensaml</groupId>
    <artifactId>opensaml-core-impl</artifactId>
    <version>${opensaml.version}</version>
    <scope>runtime</scope>
</dependency>

<!-- OpenSAML SAML API -->
<dependency>
    <groupId>org.opensaml</groupId>
    <artifactId>opensaml-saml-api</artifactId>
    <version>${opensaml.version}</version>
</dependency>

<!-- OpenSAML SAML Implementation -->
<dependency>
    <groupId>org.opensaml</groupId>
    <artifactId>opensaml-saml-impl</artifactId>
    <version>${opensaml.version}</version>
    <scope>runtime</scope>
</dependency>
```

### 5. Added Thymeleaf Template Engine

```xml
<!-- Spring Boot Starter Thymeleaf (for SAML views) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

Required for rendering HTML views in the SAML controllers (`HomeController`, `DashboardController`, etc.).

## Verification

After applying the fixes, the project compiles successfully:

```bash
cd D:\ecc-dev\jdk-21-poc\ra-web
mvn clean compile
```

**Result:**
```
[INFO] BUILD SUCCESS
```

## SAML POC Classes Now Compile Successfully

The following SAML classes are now compiling without errors:

1. **Configuration:**
   - `com.company.saml.poc.config.SAMLSecurityConfig` - SAML security configuration

2. **Controllers:**
   - `com.company.saml.poc.controller.HomeController` - Landing page
   - `com.company.saml.poc.controller.DashboardController` - Dashboard after SAML authentication
   - `com.company.saml.poc.controller.SAMLController` - SAML metadata and endpoints

3. **Model:**
   - `com.company.saml.poc.model.SAMLUser` - User model from SAML assertion

4. **Service:**
   - `com.company.saml.poc.service.SAMLUserService` - User service layer

5. **Validator:**
   - `com.company.saml.poc.validator.SAMLAssertionValidator` - SAML assertion validation (10 security checks)

6. **Utility:**
   - `com.company.saml.poc.util.SAMLMetadataGenerator` - SP metadata generation

7. **Main Application:**
   - `com.company.saml.poc.SAMLPocApplication` - Spring Boot main class

8. **Tests:**
   - `com.company.saml.poc.SAMLAuthenticationTest` - Integration tests

## Dependency Version Compatibility

| Dependency | Version | Source |
|------------|---------|--------|
| Spring Boot | 4.0.1 | Maven Central |
| Spring Security SAML2 | 7.0.2 (managed by Spring Boot) | Maven Central |
| OpenSAML | 5.1.2 | Shibboleth Repository |
| Java | 21 | - |

## Next Steps

### 1. Create SAML Keystore

The SAML configuration expects a keystore at `src/main/resources/saml-keystore.jks`:

```bash
keytool -genkeypair -alias saml-signing -keyalg RSA -keysize 2048 \
  -keystore src/main/resources/saml-keystore.jks \
  -storepass changeit -keypass changeit \
  -dname "CN=RA SAML SP, OU=IT, O=Company, C=US" \
  -validity 3650
```

### 2. Configure Application Properties

Create `src/main/resources/application-saml.yml`:

```yaml
saml:
  idp:
    metadata-url: https://idp.example.com/metadata
    entity-id: https://idp.example.com
  sp:
    entity-id: https://localhost:8443/saml/metadata
    acs-url: https://localhost:8443/login/saml2/sso/saml-poc
  keystore:
    location: classpath:saml-keystore.jks
    password: changeit
    alias: saml-signing
    key-password: changeit
```

### 3. Create Thymeleaf Templates

Create HTML templates in `src/main/resources/templates/`:
- `home.html` - Landing page
- `login.html` - Login page
- `dashboard.html` - Dashboard after authentication
- `admin.html` - Admin page (requires RA_ADMIN role)
- `officer.html` - Officer page (requires RA_OFFICER role)

### 4. Run the Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=saml
```

Or:

```bash
mvn package
java -jar target/ra-web-0.0.1-SNAPSHOT.jar --spring.profiles.active=saml
```

## Documentation Reference

For complete SAML documentation, see:
- `saml/README.md` - Navigation index
- `saml/SAML_Overview.md` - SAML concepts and advantages
- `saml/SAML_Steps_Detailed.md` - Complete 13-step authentication flow
- `saml/SAML_Java_Example.md` - Complete Java implementation examples
- `saml/SAML_POC_Quick_Start.md` - 5-minute setup guide

## Additional Fix Required

After adding dependencies, one more fix was needed:

### OpenSaml4AuthenticationProvider Error

**Problem:** `OpenSaml4AuthenticationProvider cannot be resolved to a type`

**Solution:** Spring Security 7.x uses OpenSAML 5, so the class was renamed.

Changed `OpenSaml4AuthenticationProvider` to `OpenSaml5AuthenticationProvider` in:
- `SAMLSecurityConfig.java` (line 10 - import)
- `SAMLSecurityConfig.java` (lines 164-165 - bean declaration)

**Details:** See `SAML_OPENSAML5_FIX.md` for complete information.

## Summary

✅ **Fixed:** Added all required Maven dependencies for SAML 2.0 support
✅ **Fixed:** Added Shibboleth repository for OpenSAML artifacts
✅ **Fixed:** Added Thymeleaf for view templates
✅ **Fixed:** Updated OpenSaml4 to OpenSaml5 for Spring Security 7.x compatibility
✅ **Fixed:** Added explicit Maven compiler plugin configuration for Java 21
✅ **Verified:** Project compiles successfully with `mvn compile`
✅ **Ready:** SAML POC classes are ready to run after keystore and configuration setup

## Related Documentation

- `SAML_COMPILATION_FIX.md` - This file (dependency fixes)
- `SAML_OPENSAML5_FIX.md` - OpenSAML 4 to 5 migration fix

---

**Document Version:** 1.1
**Last Updated:** 2026-01-17
**Status:** Resolved ✅
