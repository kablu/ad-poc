#!/bin/bash

echo "╔════════════════════════════════════════════════╗"
echo "║   LDAP Active Directory Test Runner           ║"
echo "╚════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Java is installed
if ! command -v javac &> /dev/null; then
    echo -e "${RED}✗ Java compiler (javac) not found!${NC}"
    echo "Please install JDK 21 or higher"
    exit 1
fi

echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"
echo ""

# Create output directory
mkdir -p target/classes/com/company/ra/service

# Test 1: Compile files
echo "═══════════════════════════════════════════════"
echo "Step 1: Compiling Java files..."
echo "═══════════════════════════════════════════════"

javac -d target/classes \
  src/main/java/com/company/ra/service/LDAPQuickTest.java \
  src/main/java/com/company/ra/service/LDAPService.java

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Compilation successful!${NC}"
else
    echo -e "${RED}✗ Compilation failed!${NC}"
    exit 1
fi
echo ""

# Test 2: Run Quick Test
echo "═══════════════════════════════════════════════"
echo "Step 2: Running Quick Connectivity Test..."
echo "═══════════════════════════════════════════════"
echo ""

java -cp target/classes com.company.ra.service.LDAPQuickTest

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Quick test completed!${NC}"
else
    echo ""
    echo -e "${RED}✗ Quick test failed!${NC}"
    echo -e "${YELLOW}Check if LDAP server is running on localhost:389${NC}"
    exit 1
fi
echo ""

# Ask user if they want to run full test
read -p "Do you want to run the full test (create user 'kablu')? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "═══════════════════════════════════════════════"
    echo "Step 3: Running Full LDAP Service Test..."
    echo "═══════════════════════════════════════════════"
    echo ""
    
    java -cp target/classes com.company.ra.service.LDAPService
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Full test completed!${NC}"
        echo ""
        echo "═══════════════════════════════════════════════"
        echo "Verify with ldapsearch:"
        echo "═══════════════════════════════════════════════"
        echo "ldapsearch -x -H ldap://localhost:389 \\"
        echo "  -D \"cn=Administrator,cn=Users,dc=corp,dc=local\" \\"
        echo "  -w \"P@ssw0rd123!\" \\"
        echo "  -b \"ou=RA Users,dc=corp,dc=local\" \\"
        echo "  \"(cn=kablu)\" cn"
    else
        echo ""
        echo -e "${RED}✗ Full test failed!${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}Skipping full test${NC}"
fi

echo ""
echo "═══════════════════════════════════════════════"
echo -e "${GREEN}✓ Test execution completed!${NC}"
echo "═══════════════════════════════════════════════"
