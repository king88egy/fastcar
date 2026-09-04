@echo off
setlocal
set DIR=%~dp0
cd /d "%DIR%"
echo Starting FastCar server on port 8080 ...
"%DIR%runtime\bin\java.exe" -jar "%DIR%FastCarServer.jar" 8080