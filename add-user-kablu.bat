@echo off
REM Script to add user 'kablu' to Active Directory
REM
REM This script compiles and runs LDAPService.java to create:
REM   Username: kablu
REM   Password: mandal
REM   OU: RA Users
REM   Domain: corp.local

echo ╔════════════════════════════════════════════════╗
echo ║   Add User 'kablu' to Active Directory        ║
echo ╚════════════════════════════════════════════════╝
echo.

REM Check Java
echo [1/3] Checking Java installation...
java -version 2>&1 | findstr "version" >nul
if errorlevel 1 (
    echo ✗ Java not found! Please install JDK 21
    pause
    exit /b 1
)
echo ✓ Java found
echo.

REM Compile
echo [2/3] Compiling LDAPService.java...
if not exist "target\classes\com\company\ra\service" mkdir "target\classes\com\company\ra\service"

javac -d target\classes ^
  src\main\java\com\company\ra\service\LDAPService.java

if errorlevel 1 (
    echo ✗ Compilation failed!
    pause
    exit /b 1
)
echo ✓ Compilation successful
echo.

REM Run
echo [3/3] Creating user 'kablu' in Active Directory...
echo.
echo ════════════════════════════════════════════════
echo.

java -cp target\classes com.company.ra.service.LDAPService

if errorlevel 1 (
    echo.
    echo ════════════════════════════════════════════════
    echo ✗ User creation failed!
    echo Please check LDAP connection settings
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════
echo ✓ Script completed!
echo ════════════════════════════════════════════════
echo.
echo Verify with ldapsearch:
echo ldapsearch -x -H ldap://localhost:389 ^
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" ^
  -w "P@ssw0rd123!" ^
  -b "ou=RA Users,dc=corp,dc=local" ^
  "(cn=kablu)" cn
echo.
pause
