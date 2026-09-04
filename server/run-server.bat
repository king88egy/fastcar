@echo off
setlocal
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0"
if errorlevel 1 exit /b 1
start "FastCar-Server" /b java -cp "out" com.fastcar.server.ServerMain > "server-console.log" 2>&1