# Samba AD Common Errors and Solutions

**Date**: 2026-01-15
**Purpose**: Quick reference for common Samba AD Docker errors

---

## Error 1: Realm Must Not Equal Domain Name

### Error Message:
```
ERROR(<class 'samba.provision.ProvisioningError'>): Provision failed -
ProvisioningError: guess_names: Realm 'COMPANY' must not be equal to
short domain name 'COMPANY'!
```

### Cause:
The NetBIOS domain name (`DOMAIN` environment variable) cannot be the same as the first part of the realm (which comes from `domainname`).

**Example of the problem:**
```yaml
domainname: sambaad.local  # Realm becomes COMPANY.LOCAL
environment:
  DOMAIN: COMPANY          # ❌ Same as realm's first part!
```

### ✅ Solution:

Change BOTH the `domainname` and `DOMAIN` to ensure they are completely different:

```yaml
domainname: sambaad.local  # Realm auto-generated as SAMBAAD.LOCAL
environment:
  DOMAIN: CORP             # ✅ Different from SAMBAAD (recommended)
```

**How Realm is Generated:**
- `domainname: sambaad.local` → Realm becomes `SAMBAAD.LOCAL`
- `domainname: example.com` → Realm becomes `EXAMPLE.COM`
- The DOMAIN (NetBIOS name) must differ from the first part of the realm

**Other valid examples:**
```yaml
DOMAIN: CORP              # ✅ Recommended (short and clear)
DOMAIN: MYCOMPANY         # ✅ Valid
DOMAIN: OFFICE            # ✅ Valid
DOMAIN: CORP01            # ✅ Valid
```

**Invalid examples:**
```yaml
DOMAIN: COMPANY           # ❌ Same as COMPANY from sambaad.local
DOMAIN: COMPANYNET        # ❌ Still contains COMPANY prefix (may fail)
```

**Naming rules:**
- NetBIOS name must be **15 characters or less**
- Uppercase letters recommended
- No special characters
- Must be different from realm's first component

### Steps to Fix:

```bash
# 1. Stop and remove existing containers
docker compose -f docker-compose-samba-ad.yml down -v

# 2. Edit docker-compose-samba-ad.yml
nano docker-compose-samba-ad.yml

# 3. Change DOMAIN value:
environment:
  DOMAIN: COMPANYNET  # or any other name

# 4. Start again
docker compose -f docker-compose-samba-ad.yml up -d

# 5. Verify it works
docker logs -f samba-ad-dc
# Should see: "samba: ready to serve connections"
```

---

## Error 2: Port Already in Use

### Error Message:
```
Error response from daemon: driver failed programming external connectivity:
failed to bind host port 0.0.0.0:53/tcp: address already in use
```

### Cause:
System service (usually `systemd-resolved`) is already using port 53.

### ✅ Solution:

**Option 1: Comment out DNS ports (Recommended)**

Already implemented in `docker-compose-samba-ad.yml`:
```yaml
ports:
  # - "53:53/tcp"    # Commented out
  # - "53:53/udp"    # Commented out
  - "389:389/tcp"    # LDAP works without DNS
  - "636:636/tcp"    # LDAPS works without DNS
```

**Option 2: Stop systemd-resolved (if you need DNS)**
```bash
sudo systemctl stop systemd-resolved
sudo systemctl disable systemd-resolved
```

See `samba-ad-port-conflict-fix.md` for detailed solutions.

---

## Error 3: Container Exits Immediately

### Error Message:
Container starts then stops, or status shows "Exited (1)"

### Check Logs:
```bash
docker logs samba-ad-dc
```

### Common Causes and Solutions:

#### Cause 1: Provisioning Error (like Realm = Domain)
**Log shows**: `Provision failed - ProvisioningError`

**Solution**: Check Error 1 above

#### Cause 2: Corrupted Volumes
**Log shows**: Database errors, permission errors

**Solution**:
```bash
# Remove volumes and start fresh
docker compose -f docker-compose-samba-ad.yml down -v
docker compose -f docker-compose-samba-ad.yml up -d
```

#### Cause 3: Insufficient Resources
**Log shows**: `Out of memory` or similar

**Solution**:
```bash
# Check Docker resources
docker stats

# Increase Docker memory (Docker Desktop: Settings > Resources)
# Recommended: 4GB RAM, 2 CPUs
```

#### Cause 4: Network Conflicts
**Log shows**: Network errors

**Solution**:
```bash
# Remove Docker networks
docker network prune

# Or change subnet in docker-compose-samba-ad.yml:
networks:
  ad-network:
    ipam:
      config:
        - subnet: 172.25.0.0/16  # Change from 172.20.0.0/16
```

---

## Error 4: LDAP Connection Refused

### Error Message:
```
ldap_sasl_bind(SIMPLE): Can't contact LDAP server (-1)
```

### Check Container Status:
```bash
# Is container running?
docker ps | grep samba-ad

# Check logs
docker logs samba-ad-dc | tail -50
```

### Solutions:

#### Solution 1: Wait for Initialization
```bash
# Samba AD takes 2-3 minutes to start
# Wait and check logs
docker logs -f samba-ad-dc

# Look for: "samba: ready to serve connections"
```

#### Solution 2: Check Port Binding
```bash
# Verify port 389 is listening
docker exec samba-ad-dc netstat -tlnp | grep 389

# Expected: tcp 0 0 0.0.0.0:389 0.0.0.0:* LISTEN
```

#### Solution 3: Test from Container
```bash
# If it works inside, it's a network issue
docker exec samba-ad-dc ldapsearch -x -H ldap://127.0.0.1 -b "dc=sambaad,dc=local"

# If this works, check firewall on host
sudo ufw status
sudo ufw allow 389/tcp
```

#### Solution 4: Check Docker Network
```bash
# Inspect network
docker network inspect ra-web_ad-network

# Container should have IP 172.20.0.10
# Try connecting to that IP
ldapsearch -x -H ldap://172.20.0.10:389 -b "dc=sambaad,dc=local"
```

---

## Error 5: Authentication Failed

### Error Message:
```
ldap_bind: Invalid credentials (49)
```

### Cause:
Wrong username, password, or DN format

### Solutions:

#### Check Administrator Password
```bash
# Default password in docker-compose: CompanyAdmin@123

# Test with correct DN format
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local"
```

#### Reset Administrator Password
```bash
docker exec -it samba-ad-dc bash
samba-tool user setpassword Administrator
# Enter new password: CompanyAdmin@123
exit
```

#### Check User Exists
```bash
# List all users
docker exec samba-ad-dc samba-tool user list

# Show specific user
docker exec samba-ad-dc samba-tool user show Administrator
```

#### Check DN Format
**❌ Wrong formats:**
```bash
-D "Administrator"  # Missing full DN
-D "Administrator@sambaad.local"  # UPN format (may not work)
-D "cn=Administrator,dc=sambaad,dc=local"  # Missing cn=Users
```

**✅ Correct format:**
```bash
-D "cn=Administrator,cn=Users,dc=sambaad,dc=local"
```

---

## Error 6: User Not Found in AD

### Error Message:
```
No such object (32)
```

### Cause:
User DN is incorrect or user doesn't exist

### Solutions:

#### List All Users
```bash
docker exec samba-ad-dc samba-tool user list
```

#### Search for User
```bash
# Search by username
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local" \
  "(sAMAccountName=jdoe)"

# If not found, create user
docker exec samba-ad-dc samba-tool user create jdoe \
  --given-name=John \
  --surname=Doe \
  --mail-address=john.doe@company.com
```

---

## Error 7: Group Membership Not Working

### Error Message:
User is in group but application doesn't recognize it

### Solutions:

#### Verify Group Membership
```bash
# List group members
docker exec samba-ad-dc samba-tool group listmembers "PKI-RA-Admins"

# Check user's memberOf attribute
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "cn=Bob Admin,cn=Users,dc=sambaad,dc=local" \
  memberOf
```

#### Re-add User to Group
```bash
docker exec samba-ad-dc bash

# Remove and re-add
samba-tool group removemembers "PKI-RA-Admins" bobadmin
samba-tool group addmembers "PKI-RA-Admins" bobadmin

# Verify
samba-tool group listmembers "PKI-RA-Admins"

exit
```

---

## Error 8: phpLDAPadmin Cannot Connect

### Error Message:
Web interface shows connection error

### Solutions:

#### Check Container Network
```bash
# Verify ldap-admin can reach samba-ad
docker exec ldap-admin ping -c 3 samba-ad

# Check DNS resolution
docker exec ldap-admin nslookup samba-ad

# Check LDAP port
docker exec ldap-admin nc -zv samba-ad 389
```

#### Restart ldap-admin
```bash
docker restart ldap-admin

# Check logs
docker logs ldap-admin
```

#### Access phpLDAPadmin
```
URL: http://localhost:8080
Login DN: cn=Administrator,cn=Users,dc=sambaad,dc=local
Password: CompanyAdmin@123
```

---

## Diagnostic Commands

### Quick Health Check
```bash
# All-in-one health check
echo "=== Container Status ==="
docker ps | grep samba

echo -e "\n=== Container Logs (last 20 lines) ==="
docker logs --tail 20 samba-ad-dc

echo -e "\n=== LDAP Connection Test ==="
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local" \
  -LLL "(objectClass=user)" cn | head -20

echo -e "\n=== Port Status ==="
sudo lsof -i :389
sudo lsof -i :636
```

### Inside Container Diagnostics
```bash
# Enter container
docker exec -it samba-ad-dc bash

# Check Samba status
samba-tool domain info 127.0.0.1

# Check DNS
nslookup sambaad.local 127.0.0.1

# Check LDAP
ldapsearch -x -H ldap://127.0.0.1 -b "dc=sambaad,dc=local" -LLL

# Check database
samba-tool dbcheck

# List users
samba-tool user list

# List groups
samba-tool group list

# Exit
exit
```

---

## Prevention Tips

### 1. Always Use Volume Flag for Clean Start
```bash
# Remove everything including data
docker compose -f docker-compose-samba-ad.yml down -v
```

### 2. Check Logs During Startup
```bash
# Watch initialization
docker logs -f samba-ad-dc

# Wait for: "samba: ready to serve connections"
```

### 3. Verify Configuration Before Starting
```bash
# Check docker-compose file syntax
docker compose -f docker-compose-samba-ad.yml config

# Should show parsed YAML without errors
```

### 4. Test LDAP Immediately After Start
```bash
# Wait 2-3 minutes, then test
sleep 180

ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local" -LLL
```

---

## Quick Reference

### Start Fresh
```bash
docker compose -f docker-compose-samba-ad.yml down -v
docker compose -f docker-compose-samba-ad.yml up -d
docker logs -f samba-ad-dc
```

### Test Connection
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local"
```

### View Logs
```bash
docker logs -f samba-ad-dc
```

### Enter Container
```bash
docker exec -it samba-ad-dc bash
```

### Reset Admin Password
```bash
docker exec -it samba-ad-dc samba-tool user setpassword Administrator
```

---

**Document End**

**Status**: Common Issues Reference Guide
**Last Updated**: 2026-01-15
