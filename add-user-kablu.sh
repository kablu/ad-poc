#!/bin/bash
# Script to add user 'kablu' to Active Directory
#
# This script compiles and runs LDAPService.java to create:
#   Username: kablu
#   Password: mandal
#   OU: RA Users
#   Domain: corp.local

echo "╔════════════════════════════════════════════════╗"
echo "║   Add User 'kablu' to Active Directory        ║"
echo "╚════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java
echo "[1/3] Checking Java installation..."
if ! command -v javac &> /dev/null; then
    echo -e "${RED}✗ Java compiler not found!${NC}"
    echo "Please install JDK 21"
    exit 1
fi
echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"
echo ""

# Compile
echo "[2/3] Compiling LDAPService.java..."
mkdir -p target/classes/com/company/ra/service

javac -d target/classes \
  src/main/java/com/company/ra/service/LDAPService.java

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Compilation failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Compilation successful${NC}"
echo ""

# Run
echo "[3/3] Creating user 'kablu' in Active Directory..."
echo ""
echo "════════════════════════════════════════════════"
echo ""

java -cp target/classes com.company.ra.service.LDAPService

if [ $? -ne 0 ]; then
    echo ""
    echo "════════════════════════════════════════════════"
    echo -e "${RED}✗ User creation failed!${NC}"
    echo "Please check LDAP connection settings"
    exit 1
fi

echo ""
echo "════════════════════════════════════════════════"
echo -e "${GREEN}✓ Script completed!${NC}"
echo "════════════════════════════════════════════════"
echo ""
echo "Verify with ldapsearch:"
echo "ldapsearch -x -H ldap://localhost:389 \\"
echo "  -D \"cn=Administrator,cn=Users,dc=corp,dc=local\" \\"
echo "  -w \"P@ssw0rd123!\" \\"
echo "  -b \"ou=RA Users,dc=corp,dc=local\" \\"
echo "  \"(cn=kablu)\" cn"
echo ""
