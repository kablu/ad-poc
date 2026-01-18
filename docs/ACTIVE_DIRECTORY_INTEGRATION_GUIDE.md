# Active Directory Integration with Java Spring Boot - Complete Guide

## Document Information
- **Document Version**: 1.0
- **Date**: 2026-01-13
- **Purpose**: Complete guide for integrating Java Spring Boot with Active Directory for authentication
- **Target Audience**: Java Developers, System Architects, System Administrators

---

## Table of Contents

1. [Active Directory Overview](#1-active-directory-overview)
2. [Active Directory Technical Architecture](#2-active-directory-technical-architecture)
3. [LDAP Protocol Deep Dive](#3-ldap-protocol-deep-dive)
4. [Prerequisites and Requirements](#4-prerequisites-and-requirements)
5. [Spring Boot AD Integration Architecture](#5-spring-boot-ad-integration-architecture)
6. [Implementation - Username/Password Validation](#6-implementation---usernamepassword-validation)
7. [REST Service Implementation](#7-rest-service-implementation)
8. [Advanced Features](#8-advanced-features)
9. [Security Best Practices](#9-security-best-practices)
10. [Troubleshooting](#10-troubleshooting)
11. [Production Considerations](#11-production-considerations)

---

## 1. Active Directory Overview

### 1.1 What is Active Directory?

**Active Directory (AD)** is Microsoft's directory service that provides:
- **Authentication**: Verify user credentials
- **Authorization**: Control access to resources
- **Directory Services**: Store and organize information about network resources
- **Policy Management**: Group policies, security settings
- **Single Sign-On (SSO)**: One login for multiple services

### 1.2 Key Concepts

#### Domain
A logical group of network objects (computers, users, devices) that share the same AD database.

**Example:**
- Domain Name: `company.com`
- FQDN: `ad.company.com`

#### Distinguished Name (DN)
Unique identifier for each object in AD.

**Format:** `CN=CommonName,OU=OrganizationalUnit,DC=DomainComponent`

**Example:**
```
CN=Kablu Ahmed,OU=Engineering,OU=Users,DC=company,DC=com
```

**Components:**
- **CN** (Common Name): User's name
- **OU** (Organizational Unit): Department/group
- **DC** (Domain Component): Domain parts

#### User Principal Name (UPN)
User-friendly login name (like email).

**Format:** `username@domain.com`

**Example:** `kablu@company.com`

#### LDAP (Lightweight Directory Access Protocol)
Protocol used to communicate with Active Directory.

**Ports:**
- **389**: LDAP (unencrypted)
- **636**: LDAPS (SSL/TLS encrypted) - **RECOMMENDED**
- **3268**: Global Catalog (LDAP)
- **3269**: Global Catalog (LDAPS)

### 1.3 Active Directory Structure

```
┌─────────────────────────────────────────────────────────────┐
│              Active Directory Structure                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Forest: company.com                                        │
│  └─ Domain: company.com                                     │
│     ├─ Domain Controllers (DC=company,DC=com)               │
│     │  ├─ dc01.company.com                                  │
│     │  └─ dc02.company.com (backup)                         │
│     │                                                        │
│     ├─ Organizational Units (OUs)                           │
│     │  ├─ OU=Users                                          │
│     │  │  ├─ OU=Engineering                                 │
│     │  │  │  └─ CN=Kablu Ahmed                             │
│     │  │  ├─ OU=HR                                          │
│     │  │  └─ OU=IT                                          │
│     │  │                                                     │
│     │  ├─ OU=Groups                                         │
│     │  │  ├─ CN=PKI-RA-Admins                              │
│     │  │  ├─ CN=PKI-RA-Officers                            │
│     │  │  └─ CN=Domain Users                               │
│     │  │                                                     │
│     │  └─ OU=Computers                                      │
│     │     ├─ CN=LAPTOP-001                                  │
│     │     └─ CN=SERVER-001                                  │
│     │                                                        │
│     └─ Schema (defines object types and attributes)         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.4 AD User Attributes

Common attributes you'll work with:

| Attribute | Description | Example |
|-----------|-------------|---------|
| `sAMAccountName` | Windows login name | `kablu` |
| `userPrincipalName` | UPN (email-like) | `kablu@company.com` |
| `distinguishedName` | Full LDAP path | `CN=Kablu Ahmed,OU=Engineering,DC=company,DC=com` |
| `mail` | Email address | `kablu.ahmed@company.com` |
| `displayName` | Full name | `Kablu Ahmed` |
| `givenName` | First name | `Kablu` |
| `sn` (surname) | Last name | `Ahmed` |
| `department` | Department | `Engineering` |
| `title` | Job title | `Software Engineer` |
| `telephoneNumber` | Phone number | `+923001234567` |
| `memberOf` | Group memberships | `CN=PKI-RA-Officers,OU=Groups,DC=company,DC=com` |
| `objectClass` | Object type | `user`, `person`, `organizationalPerson` |
| `userAccountControl` | Account status flags | `512` (enabled), `514` (disabled) |
| `pwdLastSet` | Password last changed | `133502400000000000` (Windows FILETIME) |
| `accountExpires` | Account expiration | `0` (never expires) |
| `lockoutTime` | Account lockout time | `0` (not locked) |

---

## 2. Active Directory Technical Architecture

### 2.1 LDAP Directory Information Tree (DIT)

```
Root DSE
│
├─ DC=com
│  └─ DC=company
│     ├─ CN=Users (default container)
│     │  ├─ CN=Administrator
│     │  └─ CN=Guest
│     │
│     ├─ OU=Engineering (custom OU)
│     │  ├─ CN=Kablu Ahmed (user)
│     │  ├─ CN=Saima Khan (user)
│     │  └─ CN=Development Team (group)
│     │
│     ├─ OU=Groups
│     │  ├─ CN=PKI-RA-Admins
│     │  ├─ CN=PKI-RA-Officers
│     │  └─ CN=Domain Users
│     │
│     └─ CN=Configuration (AD configuration)
```

### 2.2 Authentication Process Flow

```
┌────────────────────────────────────────────────────────────┐
│        AD Authentication Process (Detailed)                │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Step 1: Application Connects to AD                        │
│  ────────────────────────────────────────────              │
│  App → AD Server (ldaps://ad.company.com:636)             │
│  Protocol: LDAPS (SSL/TLS encrypted)                      │
│                                                            │
│  Step 2: Bind with Service Account (Optional)             │
│  ────────────────────────────────────────────              │
│  App → Binds as: CN=svc_app,OU=Service Accounts,DC=...   │
│  Purpose: Search for user account                         │
│                                                            │
│  Step 3: Search for User                                  │
│  ────────────────────────────────────────────              │
│  Search Filter: (sAMAccountName=kablu)                    │
│  or: (userPrincipalName=kablu@company.com)                │
│  Base DN: OU=Users,DC=company,DC=com                      │
│                                                            │
│  AD Returns:                                               │
│  - Distinguished Name (DN)                                 │
│  - User attributes (mail, displayName, etc.)              │
│  - Group memberships                                       │
│                                                            │
│  Step 4: Authenticate User (Bind)                         │
│  ────────────────────────────────────────────              │
│  App → Attempts to bind with user's credentials           │
│  DN: CN=Kablu Ahmed,OU=Engineering,DC=company,DC=com     │
│  Password: [user's password]                              │
│                                                            │
│  Step 5: AD Validates Credentials                         │
│  ────────────────────────────────────────────              │
│  AD checks:                                                │
│  • Password correctness                                    │
│  • Account enabled/disabled                                │
│  • Account locked out?                                     │
│  • Account expired?                                        │
│  • Password expired?                                       │
│  • Logon hours restrictions                                │
│                                                            │
│  Step 6: Authentication Result                            │
│  ────────────────────────────────────────────              │
│  Success → LDAP Bind succeeds                             │
│  Failure → LDAP error code returned                       │
│    - 49: Invalid credentials                               │
│    - 50: Insufficient access rights                        │
│    - 53: Unwilling to perform                              │
│    - 701: Account expired                                  │
│    - 773: Password must be changed                         │
│                                                            │
│  Step 7: Retrieve User Details                            │
│  ────────────────────────────────────────────              │
│  App → Queries user attributes                            │
│  Returns: Full user profile + group memberships           │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 2.3 LDAP Operations

#### Common LDAP Operations:

1. **Bind**: Authenticate to AD
2. **Search**: Find users/groups
3. **Compare**: Check attribute values
4. **Unbind**: Close connection

#### LDAP Result Codes:

| Code | Meaning | Description |
|------|---------|-------------|
| 0 | Success | Operation completed successfully |
| 1 | Operations Error | Generic error |
| 49 | Invalid Credentials | Wrong username or password |
| 50 | Insufficient Access | Insufficient permissions |
| 53 | Unwilling to Perform | Server refuses to perform |
| 68 | Already Exists | Entry already exists |
| 701 | Account Expired | User account has expired |
| 773 | Password Must Change | User must change password |
| 775 | Account Locked | Account is locked out |

---

## 3. LDAP Protocol Deep Dive

### 3.1 LDAP Search Filters

LDAP filters use a prefix notation syntax.

#### Basic Filters:

```
(attribute=value)           # Equality
(attribute=value*)          # Starts with
(attribute=*value)          # Ends with
(attribute=*value*)         # Contains
(attribute>=value)          # Greater than or equal
(attribute<=value)          # Less than or equal
```

#### Logical Operators:

```
(&(filter1)(filter2))       # AND
(|(filter1)(filter2))       # OR
(!(filter))                 # NOT
```

#### Practical Examples:

```ldap
# Find user by username
(sAMAccountName=kablu)

# Find user by email
(mail=kablu@company.com)

# Find user by UPN
(userPrincipalName=kablu@company.com)

# Find enabled users in Engineering OU
(&(objectClass=user)(!(userAccountControl:1.2.840.113556.1.4.803:=2))(department=Engineering))

# Find all groups user belongs to
(member=CN=Kablu Ahmed,OU=Engineering,DC=company,DC=com)

# Find users whose name starts with "Kab"
(displayName=Kab*)

# Find users in multiple departments
(|(department=Engineering)(department=IT))

# Find active user accounts (not disabled)
(&(objectClass=user)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))

# Find locked out accounts
(lockoutTime>=1)
```

### 3.2 User Account Control Flags

`userAccountControl` is a bit-field attribute:

| Flag | Hex | Description |
|------|-----|-------------|
| SCRIPT | 0x0001 | Logon script executed |
| ACCOUNTDISABLE | 0x0002 | Account is disabled |
| HOMEDIR_REQUIRED | 0x0008 | Home directory required |
| LOCKOUT | 0x0010 | Account is locked out |
| PASSWD_NOTREQD | 0x0020 | No password required |
| PASSWD_CANT_CHANGE | 0x0040 | User cannot change password |
| ENCRYPTED_TEXT_PWD_ALLOWED | 0x0080 | Store password with reversible encryption |
| NORMAL_ACCOUNT | 0x0200 | Normal user account |
| DONT_EXPIRE_PASSWORD | 0x10000 | Password never expires |
| PASSWORD_EXPIRED | 0x800000 | Password has expired |

**Common Values:**
- `512` (0x200): Enabled account
- `514` (0x202): Disabled account
- `66048` (0x10200): Enabled, password never expires

**Check if account is disabled:**
```ldap
(userAccountControl:1.2.840.113556.1.4.803:=2)
```

---

## 4. Prerequisites and Requirements

### 4.1 Active Directory Requirements

**What You Need from AD Administrators:**

1. **Domain Controller Information:**
   - Hostname: `ad.company.com` or IP: `192.168.1.10`
   - LDAPS Port: `636` (SSL/TLS enabled)
   - Base DN: `DC=company,DC=com`

2. **Service Account (Recommended):**
   - Username: `svc_app_ldap` or `CN=svc_app_ldap,OU=Service Accounts,DC=company,DC=com`
   - Password: Strong password
   - Permissions: Read access to user objects
   - Purpose: Search for users before authentication

3. **SSL Certificate:**
   - Export AD's SSL certificate (if using LDAPS)
   - Import into Java truststore

4. **Firewall Rules:**
   - Allow connection from application server to AD on port 636 (LDAPS)

5. **Test User Account:**
   - Username: `testuser`
   - Password: Known password
   - Purpose: Testing authentication

### 4.2 Java/Spring Boot Dependencies

**Maven `pom.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.1</version>
    </parent>

    <groupId>com.company.ra</groupId>
    <artifactId>ra-web</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring LDAP Core -->
        <dependency>
            <groupId>org.springframework.ldap</groupId>
            <artifactId>spring-ldap-core</artifactId>
        </dependency>

        <!-- Spring Security LDAP -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-ldap</artifactId>
        </dependency>

        <!-- Unboundid LDAP SDK (Alternative to Spring LDAP) -->
        <dependency>
            <groupId>com.unboundid</groupId>
            <artifactId>unboundid-ldapsdk</artifactId>
            <version>6.0.11</version>
        </dependency>

        <!-- Lombok (Optional - for cleaner code) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4.3 Application Configuration

**`application.yml`:**

```yaml
spring:
  application:
    name: ra-web

  # LDAP Configuration
  ldap:
    urls: ldaps://ad.company.com:636
    base: dc=company,dc=com
    username: CN=svc_app_ldap,OU=Service Accounts,DC=company,DC=com
    password: ${LDAP_PASSWORD:SecureServiceAccountPassword}

    # Connection pool settings
    pooled: true
    base-environment:
      java.naming.ldap.factory.socket: javax.net.ssl.SSLSocketFactory
      com.sun.jndi.ldap.connect.timeout: "5000"
      com.sun.jndi.ldap.read.timeout: "10000"
      com.sun.jndi.ldap.connect.pool: "true"
      com.sun.jndi.ldap.connect.pool.timeout: "300000"

# Active Directory specific settings
active-directory:
  domain: company.com
  url: ldaps://ad.company.com:636
  root-dn: DC=company,DC=com

  # Service account for user searches
  service-account:
    username: CN=svc_app_ldap,OU=Service Accounts,DC=company,DC=com
    password: ${AD_SERVICE_PASSWORD}

  # User search settings
  user-search-base: OU=Users,DC=company,DC=com
  user-search-filter: (sAMAccountName={0})

  # Group to role mapping
  role-mapping:
    PKI-RA-Admins: RA_ADMIN
    PKI-RA-Officers: RA_OFFICER
    PKI-RA-Operators: RA_OPERATOR
    PKI-Auditors: AUDITOR
    Domain Users: END_ENTITY

# Logging
logging:
  level:
    org.springframework.ldap: DEBUG
    org.springframework.security.ldap: DEBUG
    com.company.ra: DEBUG
```

---

## 5. Spring Boot AD Integration Architecture

### 5.1 Architecture Overview

```
┌────────────────────────────────────────────────────────────┐
│           Spring Boot AD Integration Architecture          │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────────┐                                          │
│  │ REST         │                                          │
│  │ Controller   │                                          │
│  └──────┬───────┘                                          │
│         │                                                   │
│         ▼                                                   │
│  ┌──────────────┐                                          │
│  │ Authentication│                                         │
│  │ Service      │                                          │
│  └──────┬───────┘                                          │
│         │                                                   │
│         ▼                                                   │
│  ┌──────────────┐        ┌──────────────┐                 │
│  │ AD           │───────▶│ LDAP         │                 │
│  │ Service      │        │ Template     │                 │
│  └──────┬───────┘        └──────┬───────┘                 │
│         │                       │                          │
│         │                       │ LDAPS                    │
│         │                       ▼                          │
│         │              ┌──────────────┐                    │
│         │              │ Active       │                    │
│         │              │ Directory    │                    │
│         │              └──────────────┘                    │
│         │                                                   │
│         ▼                                                   │
│  ┌──────────────┐                                          │
│  │ User         │                                          │
│  │ Details      │                                          │
│  │ Service      │                                          │
│  └──────────────┘                                          │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 6. Implementation - Username/Password Validation

### 6.1 Configuration Class

```java
package com.company.ra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ActiveDirectoryConfig {

    @Value("${active-directory.url}")
    private String ldapUrl;

    @Value("${active-directory.root-dn}")
    private String baseDn;

    @Value("${active-directory.service-account.username}")
    private String serviceUsername;

    @Value("${active-directory.service-account.password}")
    private String servicePassword;

    /**
     * LDAP Context Source - Connection to Active Directory
     */
    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();

        // Set AD server URL
        contextSource.setUrl(ldapUrl);

        // Set base DN
        contextSource.setBase(baseDn);

        // Set service account credentials for initial bind
        contextSource.setUserDn(serviceUsername);
        contextSource.setPassword(servicePassword);

        // SSL/TLS configuration
        Map<String, Object> baseEnvironment = new HashMap<>();
        baseEnvironment.put("java.naming.ldap.factory.socket",
            "javax.net.ssl.SSLSocketFactory");
        baseEnvironment.put("com.sun.jndi.ldap.connect.timeout", "5000");
        baseEnvironment.put("com.sun.jndi.ldap.read.timeout", "10000");

        // Connection pooling
        baseEnvironment.put("com.sun.jndi.ldap.connect.pool", "true");
        baseEnvironment.put("com.sun.jndi.ldap.connect.pool.timeout", "300000");

        contextSource.setBaseEnvironmentProperties(baseEnvironment);

        // Disable pooling for user authentication (security)
        contextSource.setPooled(false);

        return contextSource;
    }

    /**
     * LDAP Template - Used for LDAP operations
     */
    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }
}
```

### 6.2 Active Directory Service

```java
package com.company.ra.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.*;

@Slf4j
@Service
public class ActiveDirectoryService {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private LdapContextSource contextSource;

    @Value("${active-directory.user-search-base}")
    private String userSearchBase;

    @Value("${active-directory.domain}")
    private String domain;

    /**
     * Authenticate user against Active Directory
     *
     * @param username - sAMAccountName or userPrincipalName
     * @param password - user's password
     * @return AdUser object if authentication successful, null otherwise
     */
    public AdUser authenticateUser(String username, String password) {
        log.info("Attempting to authenticate user: {}", username);

        try {
            // Step 1: Find user in AD
            AdUser user = findUserByUsername(username);

            if (user == null) {
                log.warn("User not found in AD: {}", username);
                return null;
            }

            // Step 2: Verify user account is enabled
            if (!user.isEnabled()) {
                log.warn("User account is disabled: {}", username);
                return null;
            }

            // Step 3: Authenticate user by binding with their credentials
            boolean authenticated = authenticateWithUserCredentials(
                user.getDistinguishedName(),
                password
            );

            if (authenticated) {
                log.info("User authenticated successfully: {}", username);

                // Step 4: Retrieve full user details including groups
                user = getUserWithGroups(user.getDistinguishedName());
                return user;
            } else {
                log.warn("Authentication failed for user: {}", username);
                return null;
            }

        } catch (Exception e) {
            log.error("Error authenticating user: {}", username, e);
            return null;
        }
    }

    /**
     * Find user by username (sAMAccountName or UPN)
     */
    private AdUser findUserByUsername(String username) {
        try {
            // Create LDAP search filter
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "user"));

            // Support both sAMAccountName and userPrincipalName
            if (username.contains("@")) {
                filter.and(new EqualsFilter("userPrincipalName", username));
            } else {
                filter.and(new EqualsFilter("sAMAccountName", username));
            }

            // Search for user
            List<AdUser> users = ldapTemplate.search(
                userSearchBase,
                filter.encode(),
                SearchControls.SUBTREE_SCOPE,
                new AdUserAttributesMapper()
            );

            if (users.isEmpty()) {
                return null;
            }

            return users.get(0);

        } catch (Exception e) {
            log.error("Error finding user: {}", username, e);
            return null;
        }
    }

    /**
     * Authenticate user by attempting to bind with their credentials
     * This is the actual password validation
     */
    private boolean authenticateWithUserCredentials(String userDn, String password) {
        log.debug("Attempting bind for user DN: {}", userDn);

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, contextSource.getUrls()[0]);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        // SSL/TLS
        env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put("java.naming.ldap.factory.socket", "javax.net.ssl.SSLSocketFactory");

        // Timeouts
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "10000");

        LdapContext ctx = null;
        try {
            // Attempt to create context with user credentials
            ctx = new InitialLdapContext(env, null);

            log.debug("Bind successful for user: {}", userDn);
            return true;

        } catch (javax.naming.AuthenticationException e) {
            log.warn("Authentication failed for user: {} - Invalid credentials", userDn);
            return false;

        } catch (NamingException e) {
            log.error("LDAP error during authentication for user: {}", userDn, e);
            return false;

        } finally {
            // Always close context
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    log.error("Error closing LDAP context", e);
                }
            }
        }
    }

    /**
     * Get user with all attributes including group memberships
     */
    private AdUser getUserWithGroups(String userDn) {
        try {
            AdUser user = ldapTemplate.lookup(
                userDn,
                new AdUserAttributesMapper()
            );

            // Get group memberships
            List<String> groups = getUserGroups(userDn);
            user.setGroups(groups);

            return user;

        } catch (Exception e) {
            log.error("Error retrieving user details for: {}", userDn, e);
            return null;
        }
    }

    /**
     * Get user's group memberships
     */
    private List<String> getUserGroups(String userDn) {
        try {
            // Search for groups where user is a member
            String filter = String.format("(member=%s)", userDn);

            List<String> groups = ldapTemplate.search(
                "", // Search from root
                filter,
                SearchControls.SUBTREE_SCOPE,
                (AttributesMapper<String>) attrs -> {
                    return (String) attrs.get("cn").get();
                }
            );

            log.debug("User {} belongs to {} groups", userDn, groups.size());
            return groups;

        } catch (Exception e) {
            log.error("Error retrieving groups for user: {}", userDn, e);
            return Collections.emptyList();
        }
    }

    /**
     * AttributesMapper to map LDAP attributes to AdUser object
     */
    private static class AdUserAttributesMapper implements AttributesMapper<AdUser> {
        @Override
        public AdUser mapFromAttributes(Attributes attrs) throws NamingException {
            AdUser user = new AdUser();

            // Distinguished Name
            user.setDistinguishedName(getAttributeValue(attrs, "distinguishedName"));

            // Username
            user.setUsername(getAttributeValue(attrs, "sAMAccountName"));
            user.setUserPrincipalName(getAttributeValue(attrs, "userPrincipalName"));

            // Name attributes
            user.setDisplayName(getAttributeValue(attrs, "displayName"));
            user.setGivenName(getAttributeValue(attrs, "givenName"));
            user.setSurname(getAttributeValue(attrs, "sn"));

            // Contact info
            user.setEmail(getAttributeValue(attrs, "mail"));
            user.setTelephoneNumber(getAttributeValue(attrs, "telephoneNumber"));

            // Organizational info
            user.setDepartment(getAttributeValue(attrs, "department"));
            user.setTitle(getAttributeValue(attrs, "title"));
            user.setCompany(getAttributeValue(attrs, "company"));

            // Account status
            String uacString = getAttributeValue(attrs, "userAccountControl");
            if (uacString != null) {
                int uac = Integer.parseInt(uacString);
                // Check if account is disabled (flag 0x0002)
                user.setEnabled((uac & 0x0002) == 0);
            }

            return user;
        }

        private String getAttributeValue(Attributes attrs, String attributeName) {
            try {
                if (attrs.get(attributeName) != null) {
                    Object value = attrs.get(attributeName).get();
                    return value != null ? value.toString() : null;
                }
            } catch (NamingException e) {
                // Attribute not found or error retrieving
            }
            return null;
        }
    }
}
```

### 6.3 AD User Model

```java
package com.company.ra.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Active Directory user
 */
@Data
public class AdUser {

    // Identity
    private String distinguishedName;
    private String username;  // sAMAccountName
    private String userPrincipalName;

    // Personal info
    private String displayName;
    private String givenName;
    private String surname;
    private String email;
    private String telephoneNumber;

    // Organizational info
    private String department;
    private String title;
    private String company;

    // Account status
    private boolean enabled;

    // Group memberships
    private List<String> groups = new ArrayList<>();

    /**
     * Check if user belongs to a specific group
     */
    public boolean isMemberOf(String groupName) {
        return groups.stream()
            .anyMatch(g -> g.equalsIgnoreCase(groupName));
    }
}
```

---

## 7. REST Service Implementation

### 7.1 Authentication Request/Response DTOs

```java
package com.company.ra.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class AuthenticationRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
```

```java
package com.company.ra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private boolean authenticated;
    private String message;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private String username;
        private String displayName;
        private String email;
        private String department;
        private String title;
        private List<String> groups;
        private List<String> roles;
    }
}
```

### 7.2 REST Controller

```java
package com.company.ra.controller;

import com.company.ra.dto.AuthenticationRequest;
import com.company.ra.dto.AuthenticationResponse;
import com.company.ra.model.AdUser;
import com.company.ra.service.ActiveDirectoryService;
import com.company.ra.service.RoleMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthenticationController {

    @Autowired
    private ActiveDirectoryService adService;

    @Autowired
    private RoleMappingService roleMappingService;

    /**
     * Authenticate user against Active Directory
     *
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request) {

        log.info("Authentication request for user: {}", request.getUsername());

        try {
            // Authenticate against AD
            AdUser adUser = adService.authenticateUser(
                request.getUsername(),
                request.getPassword()
            );

            if (adUser != null) {
                // Authentication successful

                // Map AD groups to application roles
                List<String> roles = roleMappingService.mapGroupsToRoles(adUser.getGroups());

                // Build response
                AuthenticationResponse response = AuthenticationResponse.builder()
                    .authenticated(true)
                    .message("Authentication successful")
                    .user(AuthenticationResponse.UserInfo.builder()
                        .username(adUser.getUsername())
                        .displayName(adUser.getDisplayName())
                        .email(adUser.getEmail())
                        .department(adUser.getDepartment())
                        .title(adUser.getTitle())
                        .groups(adUser.getGroups())
                        .roles(roles)
                        .build())
                    .build();

                log.info("Authentication successful for user: {}", request.getUsername());
                return ResponseEntity.ok(response);

            } else {
                // Authentication failed
                AuthenticationResponse response = AuthenticationResponse.builder()
                    .authenticated(false)
                    .message("Invalid username or password")
                    .build();

                log.warn("Authentication failed for user: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            log.error("Error during authentication for user: {}",
                request.getUsername(), e);

            AuthenticationResponse response = AuthenticationResponse.builder()
                .authenticated(false)
                .message("Authentication service error")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get user information from AD (requires valid credentials)
     *
     * POST /api/v1/auth/user-info
     */
    @PostMapping("/user-info")
    public ResponseEntity<AuthenticationResponse.UserInfo> getUserInfo(
            @Valid @RequestBody AuthenticationRequest request) {

        log.info("User info request for: {}", request.getUsername());

        AdUser adUser = adService.authenticateUser(
            request.getUsername(),
            request.getPassword()
        );

        if (adUser != null) {
            List<String> roles = roleMappingService.mapGroupsToRoles(adUser.getGroups());

            AuthenticationResponse.UserInfo userInfo =
                AuthenticationResponse.UserInfo.builder()
                    .username(adUser.getUsername())
                    .displayName(adUser.getDisplayName())
                    .email(adUser.getEmail())
                    .department(adUser.getDepartment())
                    .title(adUser.getTitle())
                    .groups(adUser.getGroups())
                    .roles(roles)
                    .build();

            return ResponseEntity.ok(userInfo);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
```

### 7.3 Role Mapping Service

```java
package com.company.ra.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RoleMappingService {

    @Value("#{${active-directory.role-mapping}}")
    private Map<String, String> roleMapping;

    /**
     * Map AD groups to application roles
     *
     * Example:
     * AD Group: PKI-RA-Admins → Application Role: RA_ADMIN
     * AD Group: PKI-RA-Officers → Application Role: RA_OFFICER
     */
    public List<String> mapGroupsToRoles(List<String> adGroups) {
        List<String> roles = new ArrayList<>();

        for (String group : adGroups) {
            String role = roleMapping.get(group);
            if (role != null) {
                roles.add(role);
                log.debug("Mapped AD group '{}' to role '{}'", group, role);
            }
        }

        // Default role if no mappings found
        if (roles.isEmpty()) {
            roles.add("END_ENTITY");
            log.debug("No role mappings found, assigned default role: END_ENTITY");
        }

        return roles;
    }
}
```

---

## 8. Advanced Features

### 8.1 Using Unboundid LDAP SDK (Alternative)

**Advantages:**
- More performant
- Better connection pooling
- More control over LDAP operations

```java
package com.company.ra.service;

import com.unboundid.ldap.sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Service
public class UnboundidAdService {

    @Value("${active-directory.url}")
    private String ldapUrl;

    @Value("${active-directory.root-dn}")
    private String baseDn;

    private LDAPConnectionPool connectionPool;

    @PostConstruct
    public void init() throws LDAPException {
        // Extract host and port from URL
        String host = ldapUrl.replace("ldaps://", "").split(":")[0];
        int port = 636;

        // Create SSL connection
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
        LDAPConnection connection = new LDAPConnection(
            sslUtil.createSSLSocketFactory(),
            host,
            port
        );

        // Create connection pool
        connectionPool = new LDAPConnectionPool(connection, 10);

        log.info("LDAP connection pool initialized");
    }

    public boolean authenticateUser(String username, String password) {
        try {
            // Search for user
            String searchFilter = String.format("(sAMAccountName=%s)", username);
            SearchResult searchResult = connectionPool.search(
                baseDn,
                SearchScope.SUB,
                searchFilter,
                "distinguishedName"
            );

            if (searchResult.getEntryCount() == 0) {
                log.warn("User not found: {}", username);
                return false;
            }

            String userDn = searchResult.getSearchEntries().get(0)
                .getAttributeValue("distinguishedName");

            // Attempt bind with user credentials
            LDAPConnection userConnection = null;
            try {
                String host = ldapUrl.replace("ldaps://", "").split(":")[0];
                int port = 636;

                SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
                userConnection = new LDAPConnection(
                    sslUtil.createSSLSocketFactory(),
                    host,
                    port,
                    userDn,
                    password
                );

                log.info("Authentication successful for user: {}", username);
                return true;

            } catch (LDAPException e) {
                log.warn("Authentication failed for user: {}", username);
                return false;

            } finally {
                if (userConnection != null) {
                    userConnection.close();
                }
            }

        } catch (LDAPException e) {
            log.error("LDAP error during authentication", e);
            return false;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (connectionPool != null) {
            connectionPool.close();
            log.info("LDAP connection pool closed");
        }
    }
}
```

### 8.2 Caching AD User Information

```java
package com.company.ra.service;

import com.company.ra.model.AdUser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CachedAdService {

    @Autowired
    private ActiveDirectoryService adService;

    /**
     * Cache user information for 5 minutes
     * Cache key: username
     */
    @Cacheable(value = "adUsers", key = "#username", unless = "#result == null")
    public AdUser getUserInfo(String username) {
        // This will only call AD if not in cache
        return adService.findUserByUsername(username);
    }
}
```

**Enable caching in `application.yml`:**
```yaml
spring:
  cache:
    type: caffeine
    cache-names:
      - adUsers
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m
```

---

## 9. Security Best Practices

### 9.1 Always Use LDAPS (SSL/TLS)

**Why:** Prevent password interception

**Configuration:**
```yaml
spring:
  ldap:
    urls: ldaps://ad.company.com:636  # Use ldaps:// not ldap://
```

### 9.2 Import AD Certificate into Java Truststore

```bash
# Export certificate from AD server
openssl s_client -connect ad.company.com:636 -showcerts

# Save certificate to file: ad-cert.pem

# Import into Java truststore
keytool -import -alias ad-server -file ad-cert.pem \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit
```

### 9.3 Use Service Account with Minimal Permissions

**Service Account Should Have:**
- Read-only access to user objects
- NO write permissions
- NO administrative rights

### 9.4 Never Log Passwords

```java
// BAD:
log.info("Authenticating user: {} with password: {}", username, password);

// GOOD:
log.info("Authenticating user: {}", username);
```

### 9.5 Implement Rate Limiting

```java
@Component
public class AuthenticationRateLimiter {

    private final LoadingCache<String, AtomicInteger> attemptsCache;

    public AuthenticationRateLimiter() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });
    }

    public void recordAttempt(String username) {
        AtomicInteger attempts = attemptsCache.getUnchecked(username);
        attempts.incrementAndGet();
    }

    public boolean isAllowed(String username) {
        AtomicInteger attempts = attemptsCache.getUnchecked(username);
        return attempts.get() < 5; // Max 5 attempts per 5 minutes
    }
}
```

---

## 10. Troubleshooting

### 10.1 Common Issues and Solutions

#### Issue 1: Connection Timeout

**Error:**
```
javax.naming.CommunicationException: ad.company.com:636 [Root exception is java.net.ConnectException: Connection timed out]
```

**Solutions:**
1. Check firewall allows port 636
2. Verify AD server hostname/IP is correct
3. Test connectivity: `telnet ad.company.com 636`
4. Check AD server is running

#### Issue 2: SSL Certificate Error

**Error:**
```
javax.naming.CommunicationException: simple bind failed: ad.company.com:636 [Root exception is javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed]
```

**Solutions:**
1. Import AD certificate into Java truststore (see 9.2)
2. Or disable certificate validation (NOT recommended for production):
```java
env.put("java.naming.ldap.factory.socket", "com.company.ra.TrustAllSSLSocketFactory");
```

#### Issue 3: Authentication Failed (Valid Credentials)

**Error:**
```
Authentication failed - Invalid credentials
```

**Check:**
1. User account is enabled in AD
2. Password hasn't expired
3. Account isn't locked out
4. User DN is correct
5. Using correct username format (sAMAccountName vs UPN)

#### Issue 4: User Not Found

**Check:**
1. Search base DN is correct
2. User is in the specified OU
3. LDAP filter is correct
4. Service account has read permissions

### 10.2 Testing AD Connection

```java
@RestController
@RequestMapping("/api/v1/admin/ad")
public class AdTestController {

    @Autowired
    private ActiveDirectoryService adService;

    /**
     * Test AD connectivity
     */
    @GetMapping("/test-connection")
    public ResponseEntity<String> testConnection() {
        try {
            // Try to search for any user
            // If this succeeds, connection is working
            adService.testConnection();
            return ResponseEntity.ok("AD connection successful");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("AD connection failed: " + e.getMessage());
        }
    }
}
```

---

## 11. Production Considerations

### 11.1 High Availability

**Configure Multiple AD Servers:**
```yaml
spring:
  ldap:
    urls: ldaps://ad01.company.com:636,ldaps://ad02.company.com:636
```

### 11.2 Connection Pooling

```yaml
spring:
  ldap:
    pooled: true
    base-environment:
      com.sun.jndi.ldap.connect.pool: "true"
      com.sun.jndi.ldap.connect.pool.maxsize: "20"
      com.sun.jndi.ldap.connect.pool.prefsize: "10"
      com.sun.jndi.ldap.connect.pool.timeout: "300000"
```

### 11.3 Monitoring and Alerting

```java
@Component
public class AdHealthIndicator implements HealthIndicator {

    @Autowired
    private ActiveDirectoryService adService;

    @Override
    public Health health() {
        try {
            adService.testConnection();
            return Health.up()
                .withDetail("ad_connection", "OK")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("ad_connection", "FAILED")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 11.4 Audit Logging

```java
@Aspect
@Component
public class AuthenticationAuditAspect {

    @Autowired
    private AuditLogger auditLogger;

    @AfterReturning(
        pointcut = "execution(* com.company.ra.service.ActiveDirectoryService.authenticateUser(..))",
        returning = "result"
    )
    public void auditAuthentication(JoinPoint joinPoint, Object result) {
        String username = (String) joinPoint.getArgs()[0];
        boolean success = result != null;

        auditLogger.log(
            success ? AuditEvent.AUTH_SUCCESS : AuditEvent.AUTH_FAILURE,
            username,
            "AD authentication " + (success ? "successful" : "failed")
        );
    }
}
```

---

## Complete Example Usage

### Request:
```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "kablu",
  "password": "MySecurePassword123!"
}
```

### Response (Success):
```json
{
  "authenticated": true,
  "message": "Authentication successful",
  "user": {
    "username": "kablu",
    "displayName": "Kablu Ahmed",
    "email": "kablu@company.com",
    "department": "Engineering",
    "title": "Software Engineer",
    "groups": [
      "PKI-RA-Officers",
      "Domain Users",
      "Engineering Team"
    ],
    "roles": [
      "RA_OFFICER",
      "END_ENTITY"
    ]
  }
}
```

### Response (Failure):
```json
{
  "authenticated": false,
  "message": "Invalid username or password"
}
```

---

## Summary Checklist

### Prerequisites from AD Team:
- [ ] AD server hostname/IP: `ad.company.com`
- [ ] LDAPS port open: `636`
- [ ] Base DN: `DC=company,DC=com`
- [ ] Service account credentials
- [ ] User search base OU
- [ ] SSL certificate (for LDAPS)
- [ ] Test user credentials

### Implementation Checklist:
- [ ] Add Spring LDAP dependencies
- [ ] Configure `application.yml` with AD settings
- [ ] Create `ActiveDirectoryConfig` class
- [ ] Implement `ActiveDirectoryService`
- [ ] Create `AdUser` model
- [ ] Implement REST controller
- [ ] Add role mapping service
- [ ] Test with real AD credentials
- [ ] Implement error handling
- [ ] Add audit logging
- [ ] Configure SSL certificate trust
- [ ] Test connection failover
- [ ] Implement rate limiting
- [ ] Add health checks

---

## Additional Resources

**Microsoft Documentation:**
- [Active Directory Domain Services](https://docs.microsoft.com/en-us/windows-server/identity/ad-ds/get-started/virtual-dc/active-directory-domain-services-overview)
- [LDAP Syntax Filters](https://docs.microsoft.com/en-us/windows/win32/adsi/search-filter-syntax)

**Spring Documentation:**
- [Spring LDAP Reference](https://docs.spring.io/spring-ldap/docs/current/reference/)
- [Spring Security LDAP](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/ldap.html)

**RFC Standards:**
- [RFC 4511 - LDAP Protocol](https://datatracker.ietf.org/doc/html/rfc4511)
- [RFC 4514 - LDAP DN String Representation](https://datatracker.ietf.org/doc/html/rfc4514)
- [RFC 4515 - LDAP Search Filters](https://datatracker.ietf.org/doc/html/rfc4515)

---

**Document End**

**For Questions:**
Contact: PKI Development Team
Email: pki-dev@company.com

**Document Location:**
D:\ecc-dev\jdk-21-poc\ra-web\docs\ACTIVE_DIRECTORY_INTEGRATION_GUIDE.md
