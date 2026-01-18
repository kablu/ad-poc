@echo off
REM LDAP Active Directory Test Runner for Windows

echo ╔════════════════════════════════════════════════╗
echo ║   LDAP Active Directory Test Runner           ║
echo ╚════════════════════════════════════════════════╝
echo.

REM Check if Java is installed
javac -version >nul 2>&1
if errorlevel 1 (
    echo ✗ Java compiler not found!
    echo Please install JDK 21 or higher
    pause
    exit /b 1
)

echo ✓ Java compiler found
echo.

REM Create output directory
if not exist "target\classes\com\company\ra\service" mkdir "target\classes\com\company\ra\service"

REM Compile files
echo ═══════════════════════════════════════════════
echo Step 1: Compiling Java files...
echo ═══════════════════════════════════════════════

javac -d target\classes ^
  src\main\java\com\company\ra\service\LDAPQuickTest.java ^
  src\main\java\com\company\ra\service\LDAPService.java

if errorlevel 1 (
    echo ✗ Compilation failed!
    pause
    exit /b 1
)

echo ✓ Compilation successful!
echo.

REM Run Quick Test
echo ═══════════════════════════════════════════════
echo Step 2: Running Quick Connectivity Test...
echo ═══════════════════════════════════════════════
echo.

java -cp target\classes com.company.ra.service.LDAPQuickTest

if errorlevel 1 (
    echo.
    echo ✗ Quick test failed!
    echo Check if LDAP server is running on localhost:389
    pause
    exit /b 1
)

echo.
echo ✓ Quick test completed!
echo.

REM Ask user if they want to run full test
set /p choice="Do you want to run the full test (create user 'kablu')? (Y/N): "
if /i "%choice%"=="Y" goto runfull
if /i "%choice%"=="y" goto runfull
goto skip

:runfull
echo.
echo ═══════════════════════════════════════════════
echo Step 3: Running Full LDAP Service Test...
echo ═══════════════════════════════════════════════
echo.

java -cp target\classes com.company.ra.service.LDAPService

if errorlevel 1 (
    echo.
    echo ✗ Full test failed!
    pause
    exit /b 1
)

echo.
echo ✓ Full test completed!
echo.
echo ═══════════════════════════════════════════════
echo Verify with ldapsearch:
echo ═══════════════════════════════════════════════
echo ldapsearch -x -H ldap://localhost:389 ^
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" ^
  -w "P@ssw0rd123!" ^
  -b "ou=RA Users,dc=corp,dc=local" ^
  "(cn=kablu)" cn
goto end

:skip
echo Skipping full test
echo.

:end
echo.
echo ═══════════════════════════════════════════════
echo ✓ Test execution completed!
echo ═══════════════════════════════════════════════
pause
