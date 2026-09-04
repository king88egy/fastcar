@echo off
setlocal
set DIR=%~dp0
"%DIR%runtime\bin\java.exe" -Dfile.encoding=UTF-8 -jar "%DIR%FastCar.jar"
if errorlevel 1 pause