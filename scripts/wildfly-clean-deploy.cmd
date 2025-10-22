@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Usage: scripts\wildfly-clean-deploy.cmd "C:\Program Files\wildfly" "http://localhost:8080/api" [backendWar] [frontendWar]

set "SCRIPT_DIR=%~dp0"
set "REPO=%SCRIPT_DIR%.."

set "WILDFLY_HOME=%~1"
if "%WILDFLY_HOME%"=="" set "WILDFLY_HOME=C:\Program Files\wildfly"

set "API_BASE=%~2"
if "%API_BASE%"=="" set "API_BASE=http://localhost:8080/api"

set "BACKEND_WAR=%~3"
if "%BACKEND_WAR%"=="" set "BACKEND_WAR=%REPO%\componente-periferico\backend\target\hcen-periferico.war"

set "FRONTEND_WAR=%~4"
if "%FRONTEND_WAR%"=="" set "FRONTEND_WAR=%REPO%\componente-central\frontend-usuario-salud\target\frontend-usuario-salud.war"

echo WildFly Home: %WILDFLY_HOME%
echo API Base   : %API_BASE%
echo Backend WAR: %BACKEND_WAR%
echo Frontend WAR: %FRONTEND_WAR%

rem 1) Stop if running
call "%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --command=":shutdown" >NUL 2>&1
timeout /t 2 /nobreak >NUL

rem 2) Clean filesystem scanner remnants
set "DEPLOY_DIR=%WILDFLY_HOME%\standalone\deployments"
echo Cleaning scanner in "%DEPLOY_DIR%" ...
del /f /q "%DEPLOY_DIR%\frontend-usuario-salud.war*" >NUL 2>&1
del /f /q "%DEPLOY_DIR%\hcen-periferico.war*" >NUL 2>&1

rem 3) Start admin-only, undeploy persisted entries
echo Starting WildFly --admin-only ...
start "" "%WILDFLY_HOME%\bin\standalone.bat" --admin-only
timeout /t 6 /nobreak >NUL

echo Undeploying persisted entries (ignore errors if not present)...
call "%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --command="undeploy frontend-usuario-salud.war --keep-content=false" >NUL 2>&1
call "%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --command="undeploy hcen-periferico.war --keep-content=false" >NUL 2>&1

echo Shutting down admin-only ...
call "%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --command=":shutdown" >NUL 2>&1
timeout /t 3 /nobreak >NUL

rem 4) Start normal with frontend props
echo Starting WildFly normal with -Dhcen.apiBaseUrl and -Dhcen.enableDevLogin ...
start "" "%WILDFLY_HOME%\bin\standalone.bat" -Dhcen.apiBaseUrl=%API_BASE% -Dhcen.enableDevLogin=true
timeout /t 6 /nobreak >NUL

rem 5) Deploy via CLI (single method)
echo Deploying backend WAR ...
call "%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --command="deploy \"%BACKEND_WAR%\" --force"
if errorlevel 1 goto :fail

echo Deploying frontend WAR ...
call "%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --command="deploy \"%FRONTEND_WAR%\" --force"
if errorlevel 1 goto :fail

echo Done. Test API: %API_BASE%/historia/{CI}/documentos
exit /b 0

:fail
echo Deployment failed. Check WildFly logs under "%WILDFLY_HOME%\standalone\log\server.log".
exit /b 1

