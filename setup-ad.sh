#!/bin/bash

##############################################################################
# Active Directory Docker Setup Script
# Purpose: One-command installation of Samba AD on Ubuntu
# Date: 2026-01-16
##############################################################################

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="ad-docker-compose.yml"
CONTAINER_NAME="ad-dc"

##############################################################################
# Helper Functions
##############################################################################

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed!"
        print_info "Please install Docker first. Run: sudo apt-get install docker.io docker-compose-plugin"
        exit 1
    fi
    print_success "Docker is installed"
}

check_compose() {
    if ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed!"
        print_info "Please install Docker Compose plugin. Run: sudo apt-get install docker-compose-plugin"
        exit 1
    fi
    print_success "Docker Compose is installed"
}

check_compose_file() {
    if [ ! -f "$COMPOSE_FILE" ]; then
        print_error "Docker Compose file not found: $COMPOSE_FILE"
        print_info "Please ensure $COMPOSE_FILE is in the current directory"
        exit 1
    fi
    print_success "Docker Compose file found"
}

check_ports() {
    print_info "Checking if required ports are available..."

    for port in 389 636 8080; do
        if sudo lsof -i :$port -t &> /dev/null; then
            print_warning "Port $port is already in use"
            print_info "Process using port $port:"
            sudo lsof -i :$port
            read -p "Do you want to continue anyway? (y/n): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                print_error "Setup cancelled"
                exit 1
            fi
        else
            print_success "Port $port is available"
        fi
    done
}

start_ad() {
    print_header "Starting Active Directory"

    # Stop and remove existing containers
    print_info "Cleaning up old containers..."
    docker compose -f "$COMPOSE_FILE" down 2>/dev/null || true

    # Start new containers
    print_info "Starting containers..."
    docker compose -f "$COMPOSE_FILE" up -d

    print_success "Containers started"
}

wait_for_ad() {
    print_header "Waiting for AD to initialize"
    print_info "This may take 2-3 minutes..."

    local max_wait=180  # 3 minutes
    local waited=0
    local ready=false

    while [ $waited -lt $max_wait ]; do
        if docker logs "$CONTAINER_NAME" 2>&1 | grep -q "samba: ready to serve connections"; then
            ready=true
            break
        fi

        echo -n "."
        sleep 5
        waited=$((waited + 5))
    done

    echo ""

    if [ "$ready" = true ]; then
        print_success "Active Directory is ready!"
    else
        print_warning "Timeout waiting for AD initialization"
        print_info "Check logs with: docker logs $CONTAINER_NAME"
        print_info "AD may still be initializing in the background"
    fi
}

test_ldap_connection() {
    print_header "Testing LDAP Connection"

    # Check if ldapsearch is available
    if ! command -v ldapsearch &> /dev/null; then
        print_warning "ldapsearch not found. Skipping LDAP test."
        print_info "Install with: sudo apt-get install ldap-utils"
        return
    fi

    # Wait a bit more for LDAP to be ready
    sleep 5

    print_info "Testing LDAP connection..."
    if docker exec "$CONTAINER_NAME" smbclient -L localhost -U "%" &> /dev/null; then
        print_success "LDAP is responding"
    else
        print_warning "LDAP may not be fully ready yet"
        print_info "Wait a minute and test with: docker logs $CONTAINER_NAME"
    fi
}

create_test_users() {
    print_header "Creating Test Users and Groups"

    print_info "Creating users: jdoe, alice, bobadmin"

    docker exec "$CONTAINER_NAME" bash -c "
        samba-tool user create jdoe Password@123 --given-name=John --surname=Doe --mail-address=john.doe@sambaad.local 2>/dev/null || echo 'User jdoe may already exist'
        samba-tool user create alice Password@123 --given-name=Alice --surname=Smith --mail-address=alice@sambaad.local 2>/dev/null || echo 'User alice may already exist'
        samba-tool user create bobadmin Password@123 --given-name=Bob --surname=Admin --mail-address=bob.admin@sambaad.local 2>/dev/null || echo 'User bobadmin may already exist'
    " > /dev/null

    print_success "Users created"

    print_info "Creating groups: PKI-RA-Admins, PKI-RA-Officers, PKI-RA-Operators, PKI-Auditors"

    docker exec "$CONTAINER_NAME" bash -c "
        samba-tool group add 'PKI-RA-Admins' --description='RA Administrators' 2>/dev/null || echo 'Group PKI-RA-Admins may already exist'
        samba-tool group add 'PKI-RA-Officers' --description='RA Officers' 2>/dev/null || echo 'Group PKI-RA-Officers may already exist'
        samba-tool group add 'PKI-RA-Operators' --description='RA Operators' 2>/dev/null || echo 'Group PKI-RA-Operators may already exist'
        samba-tool group add 'PKI-Auditors' --description='PKI Auditors' 2>/dev/null || echo 'Group PKI-Auditors may already exist'
    " > /dev/null

    print_success "Groups created"

    print_info "Adding users to groups"

    docker exec "$CONTAINER_NAME" bash -c "
        samba-tool group addmembers 'PKI-RA-Admins' bobadmin 2>/dev/null || true
        samba-tool group addmembers 'PKI-RA-Officers' alice 2>/dev/null || true
        samba-tool group addmembers 'PKI-RA-Operators' jdoe 2>/dev/null || true
    " > /dev/null

    print_success "Users added to groups"
}

show_connection_info() {
    print_header "Active Directory Connection Information"

    cat << EOF

${GREEN}Active Directory is ready!${NC}

${BLUE}Connection Parameters:${NC}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
LDAP URL:      ldap://localhost:389
LDAPS URL:     ldaps://localhost:636
Base DN:       dc=sambaad,dc=local
Admin DN:      cn=Administrator,cn=Users,dc=sambaad,dc=local
Admin Pass:    Admin@123456
Domain:        sambaad.local
Realm:         SAMBAAD.LOCAL
NetBIOS:       CORP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

${BLUE}Web Interface:${NC}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
phpLDAPadmin:  ${GREEN}http://localhost:8080${NC}
  Login DN:    cn=Administrator,cn=Users,dc=sambaad,dc=local
  Password:    Admin@123456
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

${BLUE}Test Users:${NC}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Username      Password       Group               DN
─────────────────────────────────────────────────────────
Administrator Admin@123456   Domain Admins       cn=Administrator,cn=Users,dc=sambaad,dc=local
jdoe          Password@123   PKI-RA-Operators    cn=John Doe,cn=Users,dc=sambaad,dc=local
alice         Password@123   PKI-RA-Officers     cn=Alice Smith,cn=Users,dc=sambaad,dc=local
bobadmin      Password@123   PKI-RA-Admins       cn=Bob Admin,cn=Users,dc=sambaad,dc=local
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

${BLUE}Useful Commands:${NC}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
View logs:          docker logs -f $CONTAINER_NAME
Enter container:    docker exec -it $CONTAINER_NAME bash
List users:         docker exec $CONTAINER_NAME samba-tool user list
Stop AD:            docker compose -f $COMPOSE_FILE stop
Start AD:           docker compose -f $COMPOSE_FILE start
Restart AD:         docker compose -f $COMPOSE_FILE restart
Remove AD:          docker compose -f $COMPOSE_FILE down -v
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

${YELLOW}For more information, see: ad-setup-README.md${NC}

EOF
}

##############################################################################
# Main Script
##############################################################################

main() {
    print_header "Active Directory Docker Setup"

    # Pre-flight checks
    print_info "Running pre-flight checks..."
    check_docker
    check_compose
    check_compose_file
    check_ports

    # Start AD
    start_ad

    # Wait for initialization
    wait_for_ad

    # Test connection
    test_ldap_connection

    # Create test users (optional)
    read -p "Do you want to create test users and groups? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        create_test_users
    fi

    # Show connection info
    show_connection_info

    print_success "Setup complete!"
}

# Run main function
main "$@"
