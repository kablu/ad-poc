# SAML Authentication Steps - Detailed Guide

## Table of Contents
1. [SAML Authentication Flow - High Level](#saml-authentication-flow---high-level)
2. [Detailed Step-by-Step Process](#detailed-step-by-step-process)
3. [SP-Initiated SSO Flow](#sp-initiated-sso-flow)
4. [IdP-Initiated SSO Flow](#idp-initiated-sso-flow)
5. [SAML Logout Flow](#saml-logout-flow)
6. [Technical Implementation Steps](#technical-implementation-steps)

---

## SAML Authentication Flow - High Level

### The 3 Main Phases:

```
Phase 1: Request          Phase 2: Authentication       Phase 3: Assertion
┌──────────────┐         ┌──────────────┐              ┌──────────────┐
│    User      │         │   Identity   │              │   Service    │
│  Requests    │────────>│   Provider   │─────────────>│   Provider   │
│   Access     │         │ Authenticates│              │  Validates   │
└──────────────┘         └──────────────┘              └──────────────┘
```

---

## Detailed Step-by-Step Process

### Complete SAML SSO Flow (13 Steps)

```
┌─────────┐                ┌─────────┐                ┌─────────┐
│  User   │                │   SP    │                │   IdP   │
│ Browser │                │ (RA Web)│                │   (AD)  │
└────┬────┘                └────┬────┘                └────┬────┘
     │                          │                          │
     │ STEP 1: Access Protected Resource                  │
     │ GET https://ra.company.com/dashboard               │
     ├─────────────────────────>│                          │
     │                          │                          │
     │                          │ STEP 2: Check Authentication
     │                          │ - No valid session found │
     │                          │ - User not authenticated │
     │                          │                          │
     │ STEP 3: Redirect to IdP  │                          │
     │ 302 Redirect             │                          │
     │<─────────────────────────┤                          │
     │ Location: https://idp.example.com/saml/sso?SAMLRequest=...
     │                          │                          │
     │                          │                          │
     │ STEP 4: Follow Redirect with SAML AuthnRequest     │
     │ GET https://idp.example.com/saml/sso               │
     ├────────────────────────────────────────────────────>│
     │ SAMLRequest (Base64 encoded, deflated):            │
     │ <samlp:AuthnRequest                                │
     │   ID="_request_12345"                              │
     │   IssueInstant="2026-01-16T10:00:00Z"              │
     │   AssertionConsumerServiceURL=                     │
     │     "https://ra.company.com/saml/acs">             │
     │   <saml:Issuer>https://ra.company.com</saml:Issuer>│
     │   <samlp:NameIDPolicy                              │
     │     Format="emailAddress"                          │
     │     AllowCreate="true"/>                           │
     │ </samlp:AuthnRequest>                              │
     │                          │                          │
     │                          │                          │
     │                          │ STEP 5: IdP Validates Request
     │                          │ - Verify SAMLRequest signature
     │                          │ - Check Issuer (SP entity ID)
     │                          │ - Validate ACS URL        │
     │                          │                          │
     │                          │                          │
     │ STEP 6: Check if User Already Authenticated        │
     │ - IdP checks for existing session                  │
     │ - If session exists, skip to Step 9                │
     │ - If no session, show login form                   │
     │<────────────────────────────────────────────────────┤
     │ 200 OK - Login Form HTML                           │
     │                          │                          │
     │                          │                          │
     │ STEP 7: User Submits Credentials                   │
     │ POST https://idp.example.com/login                 │
     ├────────────────────────────────────────────────────>│
     │ username=john.doe@company.com                      │
     │ password=********                                  │
     │                          │                          │
     │                          │                          │
     │                          │ STEP 8: IdP Authenticates User
     │                          │ - Verify credentials against AD
     │                          │ - Check account status    │
     │                          │ - Retrieve user attributes│
     │                          │ - Retrieve group memberships
     │                          │ - Map groups to roles     │
     │                          │ - Create IdP session      │
     │                          │                          │
     │                          │                          │
     │                          │ STEP 9: Generate SAML Assertion
     │                          │ - Create Assertion XML    │
     │                          │ - Add user attributes:    │
     │                          │   * NameID (email)        │
     │                          │   * firstName, lastName   │
     │                          │   * department, role      │
     │                          │ - Set conditions (validity)│
     │                          │ - Add audience restriction│
     │                          │ - Digitally sign assertion│
     │                          │ - Optional: Encrypt assertion
     │                          │                          │
     │                          │                          │
     │ STEP 10: Send SAML Response to Browser             │
     │<────────────────────────────────────────────────────┤
     │ 200 OK - HTML Form with auto-submit                │
     │ <form method="POST"                                │
     │   action="https://ra.company.com/saml/acs">        │
     │   <input type="hidden" name="SAMLResponse"         │
     │     value="base64-encoded-saml-response"/>         │
     │ </form>                                            │
     │ <script>document.forms[0].submit();</script>       │
     │                          │                          │
     │                          │                          │
     │ STEP 11: Browser Auto-POSTs SAML Response to SP    │
     │ POST https://ra.company.com/saml/acs               │
     ├─────────────────────────>│                          │
     │ SAMLResponse=base64-encoded-xml                    │
     │                          │                          │
     │                          │                          │
     │                          │ STEP 12: SP Validates SAML Response
     │                          │ ① Parse SAML Response XML │
     │                          │ ② Verify digital signature│
     │                          │ ③ Check Issuer = IdP      │
     │                          │ ④ Validate conditions:    │
     │                          │    - NotBefore timestamp  │
     │                          │    - NotOnOrAfter timestamp
     │                          │ ⑤ Check Audience = SP     │
     │                          │ ⑥ Verify Subject Confirmation
     │                          │ ⑦ Check for replay (Assertion ID)
     │                          │ ⑧ Extract user attributes │
     │                          │ ⑨ Map roles to authorities│
     │                          │ ⑩ Create local session    │
     │                          │                          │
     │                          │                          │
     │ STEP 13: Grant Access & Redirect to Original Resource
     │ 302 Redirect to /dashboard                         │
     │<─────────────────────────┤                          │
     │                          │                          │
     │                          │                          │
     │ STEP 14: Access Protected Resource with Session    │
     │ GET https://ra.company.com/dashboard               │
     ├─────────────────────────>│                          │
     │                          │                          │
     │ 200 OK - Dashboard Page  │                          │
     │<─────────────────────────┤                          │
     │                          │                          │
```

---

## SP-Initiated SSO Flow

### Scenario: User starts at Service Provider (RA Web Application)

#### Step 1: User Accesses Protected Resource
```http
GET /dashboard HTTP/1.1
Host: ra.company.com
```

**What Happens:**
- User navigates to protected resource
- SP checks for valid session
- No session found → Initiate SAML authentication

#### Step 2: SP Generates SAML AuthnRequest
```xml
<samlp:AuthnRequest
    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
    ID="_request_abc123"
    Version="2.0"
    IssueInstant="2026-01-16T10:00:00Z"
    Destination="https://idp.example.com/saml/sso"
    AssertionConsumerServiceURL="https://ra.company.com/saml/acs"
    ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST">

    <saml:Issuer>https://ra.company.com</saml:Issuer>

    <samlp:NameIDPolicy
        Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
        AllowCreate="true"/>

    <samlp:RequestedAuthnContext Comparison="exact">
        <saml:AuthnContextClassRef>
            urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
        </saml:AuthnContextClassRef>
    </samlp:RequestedAuthnContext>
</samlp:AuthnRequest>
```

**What Happens:**
- SP creates AuthnRequest with unique ID
- Includes return URL (AssertionConsumerServiceURL)
- Specifies requested NameID format (email)
- Optionally signs the request

#### Step 3: Redirect User to IdP
```http
HTTP/1.1 302 Found
Location: https://idp.example.com/saml/sso?SAMLRequest=base64-deflated-request&RelayState=dashboard
```

**What Happens:**
- SP encodes AuthnRequest (Base64 + Deflate)
- Redirects browser to IdP SSO endpoint
- Includes RelayState to remember original destination

#### Step 4: User Arrives at IdP
```http
GET /saml/sso?SAMLRequest=...&RelayState=dashboard HTTP/1.1
Host: idp.example.com
```

**What Happens:**
- Browser follows redirect to IdP
- IdP receives and decodes SAMLRequest
- IdP validates request (signature, issuer, timestamp)

#### Step 5: IdP Checks Existing Session
**What Happens:**
- IdP checks for existing authenticated session cookie
- If session exists → Skip to Step 8 (Generate Assertion)
- If no session → Show login form

#### Step 6: User Authenticates at IdP
```http
POST /login HTTP/1.1
Host: idp.example.com
Content-Type: application/x-www-form-urlencoded

username=john.doe@company.com&password=SecurePassword123
```

**What Happens:**
- User submits credentials
- IdP validates against Active Directory
- IdP may require MFA (second factor)
- IdP creates session cookie

#### Step 7: IdP Retrieves User Attributes
**LDAP Query to Active Directory:**
```
Search Base: DC=company,DC=com
Filter: (userPrincipalName=john.doe@company.com)
Attributes: displayName, mail, department, memberOf
```

**Result:**
```
displayName: John Doe
mail: john.doe@company.com
department: Engineering
memberOf: CN=PKI-RA-Officers,OU=Groups,DC=company,DC=com
```

#### Step 8: IdP Maps Groups to Roles
```
AD Group: "CN=PKI-RA-Officers,..." → SAML Role: "RA_OFFICER"
```

#### Step 9: IdP Generates SAML Assertion
```xml
<saml:Assertion
    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
    ID="_assertion_xyz789"
    Version="2.0"
    IssueInstant="2026-01-16T10:01:00Z">

    <saml:Issuer>https://idp.example.com</saml:Issuer>

    <!-- Digital Signature -->
    <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:SignedInfo>
            <ds:CanonicalizationMethod Algorithm="..."/>
            <ds:SignatureMethod Algorithm="...RSA-SHA256"/>
            <ds:Reference URI="#_assertion_xyz789">
                <ds:DigestMethod Algorithm="...SHA256"/>
                <ds:DigestValue>abc123...</ds:DigestValue>
            </ds:Reference>
        </ds:SignedInfo>
        <ds:SignatureValue>base64-signature...</ds:SignatureValue>
        <ds:KeyInfo>
            <ds:X509Data>
                <ds:X509Certificate>MII...</ds:X509Certificate>
            </ds:X509Data>
        </ds:KeyInfo>
    </ds:Signature>

    <!-- Subject (User Identity) -->
    <saml:Subject>
        <saml:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress">
            john.doe@company.com
        </saml:NameID>
        <saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
            <saml:SubjectConfirmationData
                NotOnOrAfter="2026-01-16T10:06:00Z"
                Recipient="https://ra.company.com/saml/acs"
                InResponseTo="_request_abc123"/>
        </saml:SubjectConfirmation>
    </saml:Subject>

    <!-- Conditions (Validity Window) -->
    <saml:Conditions
        NotBefore="2026-01-16T10:00:00Z"
        NotOnOrAfter="2026-01-16T10:06:00Z">
        <saml:AudienceRestriction>
            <saml:Audience>https://ra.company.com</saml:Audience>
        </saml:AudienceRestriction>
    </saml:Conditions>

    <!-- Authentication Statement -->
    <saml:AuthnStatement
        AuthnInstant="2026-01-16T10:01:00Z"
        SessionIndex="_session_session123">
        <saml:AuthnContext>
            <saml:AuthnContextClassRef>
                urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
            </saml:AuthnContextClassRef>
        </saml:AuthnContext>
    </saml:AuthnStatement>

    <!-- Attribute Statement (User Attributes) -->
    <saml:AttributeStatement>
        <saml:Attribute Name="email">
            <saml:AttributeValue>john.doe@company.com</saml:AttributeValue>
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

#### Step 10: IdP Wraps Assertion in SAML Response
```xml
<samlp:Response
    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
    ID="_response_def456"
    Version="2.0"
    IssueInstant="2026-01-16T10:01:00Z"
    Destination="https://ra.company.com/saml/acs"
    InResponseTo="_request_abc123">

    <saml:Issuer>https://idp.example.com</saml:Issuer>

    <samlp:Status>
        <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
    </samlp:Status>

    <!-- Assertion from Step 9 -->
    <saml:Assertion>...</saml:Assertion>
</samlp:Response>
```

#### Step 11: IdP Sends HTML Form to Browser
```html
<!DOCTYPE html>
<html>
<head><title>SAML POST</title></head>
<body onload="document.forms[0].submit()">
    <form method="POST" action="https://ra.company.com/saml/acs">
        <input type="hidden" name="SAMLResponse"
               value="base64-encoded-saml-response"/>
        <input type="hidden" name="RelayState"
               value="dashboard"/>
        <noscript>
            <input type="submit" value="Continue"/>
        </noscript>
    </form>
</body>
</html>
```

**What Happens:**
- IdP returns HTML page with auto-submit form
- JavaScript automatically POSTs form to SP
- SAMLResponse is Base64-encoded (not deflated)

#### Step 12: Browser POSTs SAML Response to SP
```http
POST /saml/acs HTTP/1.1
Host: ra.company.com
Content-Type: application/x-www-form-urlencoded

SAMLResponse=base64-encoded-response&RelayState=dashboard
```

#### Step 13: SP Validates SAML Response

**Validation Checklist:**

1. **Parse XML**
   ```java
   Document doc = parseXML(base64Decode(samlResponse));
   ```

2. **Verify Digital Signature**
   ```java
   boolean valid = verifySignature(doc, idpCertificate);
   if (!valid) throw new SecurityException("Invalid signature");
   ```

3. **Check Issuer**
   ```java
   String issuer = doc.getIssuer();
   if (!issuer.equals("https://idp.example.com")) {
       throw new SecurityException("Unknown issuer");
   }
   ```

4. **Validate Timestamps (NotBefore/NotOnOrAfter)**
   ```java
   Instant now = Instant.now();
   Instant notBefore = doc.getNotBefore();
   Instant notOnOrAfter = doc.getNotOnOrAfter();

   if (now.isBefore(notBefore) || now.isAfter(notOnOrAfter)) {
       throw new SecurityException("Assertion expired");
   }
   ```

5. **Check Audience**
   ```java
   String audience = doc.getAudience();
   if (!audience.equals("https://ra.company.com")) {
       throw new SecurityException("Invalid audience");
   }
   ```

6. **Verify InResponseTo (matches original request ID)**
   ```java
   String inResponseTo = doc.getInResponseTo();
   if (!pendingRequests.contains(inResponseTo)) {
       throw new SecurityException("Unsolicited response");
   }
   ```

7. **Check for Assertion Replay**
   ```java
   String assertionId = doc.getAssertionId();
   if (usedAssertionIds.contains(assertionId)) {
       throw new SecurityException("Assertion replay detected");
   }
   usedAssertionIds.add(assertionId);
   ```

8. **Extract User Attributes**
   ```java
   String email = doc.getAttribute("email");
   String firstName = doc.getAttribute("firstName");
   String role = doc.getAttribute("role");
   ```

#### Step 14: SP Creates Local Session
```java
HttpSession session = request.getSession(true);
session.setAttribute("user.email", email);
session.setAttribute("user.role", role);
session.setAttribute("saml.sessionIndex", sessionIndex);
session.setMaxInactiveInterval(3600); // 1 hour
```

#### Step 15: Redirect to Original Resource
```http
HTTP/1.1 302 Found
Location: /dashboard
Set-Cookie: JSESSIONID=abc123; Path=/; HttpOnly; Secure; SameSite=Strict
```

#### Step 16: User Accesses Dashboard
```http
GET /dashboard HTTP/1.1
Host: ra.company.com
Cookie: JSESSIONID=abc123
```

**Result:** User successfully authenticated and accessing protected resource!

---

## IdP-Initiated SSO Flow

### Scenario: User starts at Identity Provider

#### Step 1: User Logs into IdP Portal
```
User navigates to: https://idp.example.com/portal
User authenticates with username/password
```

#### Step 2: User Clicks on Application Link
```
IdP shows available applications:
- RA Web Application
- Email
- HR System

User clicks "RA Web Application"
```

#### Step 3: IdP Generates SAML Assertion
```xml
<!-- Same as SP-initiated, but without InResponseTo attribute -->
<saml:Assertion ID="_assertion_xyz789" ...>
    <saml:Subject>
        <saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
            <saml:SubjectConfirmationData
                NotOnOrAfter="2026-01-16T10:06:00Z"
                Recipient="https://ra.company.com/saml/acs"/>
                <!-- NO InResponseTo attribute -->
        </saml:SubjectConfirmation>
    </saml:Subject>
    ...
</saml:Assertion>
```

#### Step 4: IdP POSTs Assertion to SP
```html
<form method="POST" action="https://ra.company.com/saml/acs">
    <input type="hidden" name="SAMLResponse" value="..."/>
</form>
```

#### Step 5: SP Validates and Creates Session
```
Same validation as SP-initiated flow, except:
- No InResponseTo check (unsolicited response allowed)
- Must verify this SP accepts IdP-initiated SSO
```

---

## SAML Logout Flow

### Single Logout (SLO) - SP-Initiated

#### Step 1: User Clicks Logout at SP
```http
GET /logout HTTP/1.1
Host: ra.company.com
Cookie: JSESSIONID=abc123
```

#### Step 2: SP Generates SAML LogoutRequest
```xml
<samlp:LogoutRequest
    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
    ID="_logout_request_123"
    Version="2.0"
    IssueInstant="2026-01-16T11:00:00Z"
    Destination="https://idp.example.com/saml/slo">

    <saml:Issuer>https://ra.company.com</saml:Issuer>

    <saml:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress">
        john.doe@company.com
    </saml:NameID>

    <samlp:SessionIndex>_session_session123</samlp:SessionIndex>
</samlp:LogoutRequest>
```

#### Step 3: Redirect to IdP SLO Endpoint
```http
HTTP/1.1 302 Found
Location: https://idp.example.com/saml/slo?SAMLRequest=...
```

#### Step 4: IdP Processes Logout
```
1. Validate LogoutRequest signature
2. Find user session by SessionIndex
3. Invalidate IdP session
4. Optionally notify other SPs with active sessions
```

#### Step 5: IdP Sends LogoutResponse
```xml
<samlp:LogoutResponse
    ID="_logout_response_456"
    InResponseTo="_logout_request_123"
    IssueInstant="2026-01-16T11:00:10Z">

    <saml:Issuer>https://idp.example.com</saml:Issuer>

    <samlp:Status>
        <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
    </samlp:Status>
</samlp:LogoutResponse>
```

#### Step 6: SP Destroys Local Session
```java
session.invalidate();
```

#### Step 7: Redirect to Logout Success Page
```http
HTTP/1.1 302 Found
Location: /login?logout=success
```

---

## Technical Implementation Steps

### Setup Phase (One-Time Configuration)

#### Step 1: Generate SP Certificate for Signing
```bash
# Generate private key and certificate
keytool -genkeypair -alias saml-signing \
    -keyalg RSA -keysize 2048 \
    -keystore saml-keystore.jks \
    -storepass keystorePassword \
    -keypass keyPassword \
    -dname "CN=ra.company.com, OU=IT, O=Company, C=US" \
    -validity 3650
```

#### Step 2: Export SP Public Certificate
```bash
keytool -export -alias saml-signing \
    -file sp-certificate.cer \
    -keystore saml-keystore.jks \
    -storepass keystorePassword
```

#### Step 3: Generate SP Metadata
```xml
<?xml version="1.0"?>
<EntityDescriptor entityID="https://ra.company.com"
    xmlns="urn:oasis:names:tc:SAML:2.0:metadata">

    <SPSSODescriptor
        AuthnRequestsSigned="true"
        WantAssertionsSigned="true"
        protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">

        <!-- Signing Certificate -->
        <KeyDescriptor use="signing">
            <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#">
                <X509Data>
                    <X509Certificate>MII...</X509Certificate>
                </X509Data>
            </KeyInfo>
        </KeyDescriptor>

        <!-- Assertion Consumer Service -->
        <AssertionConsumerService
            Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
            Location="https://ra.company.com/saml/acs"
            index="0" isDefault="true"/>

        <!-- Single Logout Service -->
        <SingleLogoutService
            Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
            Location="https://ra.company.com/saml/slo"/>

    </SPSSODescriptor>
</EntityDescriptor>
```

#### Step 4: Exchange Metadata
```
1. Send SP metadata to IdP administrator
2. Receive IdP metadata from IdP administrator
3. Configure SP to trust IdP certificate
4. Configure IdP to trust SP certificate
```

#### Step 5: Configure Attribute Mapping
```yaml
# application.yml
saml:
  attribute-mapping:
    email: "urn:oid:0.9.2342.19200300.100.1.3"
    firstName: "urn:oid:2.5.4.42"
    lastName: "urn:oid:2.5.4.4"
    department: "urn:oid:2.5.4.11"
    role: "http://schemas.xmlsoap.org/claims/Group"
```

#### Step 6: Test SAML Integration
```
1. Access protected resource
2. Verify redirect to IdP
3. Authenticate at IdP
4. Verify successful callback to SP
5. Check user attributes extracted correctly
6. Test logout flow
```

---

## Summary of Key Steps

### Authentication Flow (13 Steps):
1. User requests protected resource at SP
2. SP checks for session (not found)
3. SP generates SAML AuthnRequest
4. SP redirects user to IdP with request
5. IdP validates AuthnRequest
6. IdP checks for existing session
7. User authenticates at IdP (if needed)
8. IdP retrieves user attributes from AD
9. IdP generates digitally signed SAML Assertion
10. IdP sends HTML form with SAMLResponse
11. Browser auto-POSTs to SP's ACS endpoint
12. SP validates SAML Response (signature, timestamps, audience, etc.)
13. SP creates local session and grants access

### Validation Steps at SP (10 Checks):
1. Parse XML
2. Verify digital signature
3. Check issuer matches IdP
4. Validate NotBefore timestamp
5. Validate NotOnOrAfter timestamp
6. Check audience restriction
7. Verify InResponseTo (for SP-initiated)
8. Detect assertion replay
9. Extract user attributes
10. Create authenticated session

### Logout Flow (7 Steps):
1. User initiates logout at SP
2. SP generates LogoutRequest
3. SP redirects to IdP SLO endpoint
4. IdP invalidates session
5. IdP sends LogoutResponse
6. SP destroys local session
7. Redirect to logout success page

This comprehensive flow ensures secure, federated authentication across enterprise applications!
