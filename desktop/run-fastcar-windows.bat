@echo off
setlocal
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
echo Starting Fast Car (Windows)...
java -cp "%~dp0out" com.fastcar.desktop.App
if errorlevel 1 (
  echo.
  echo Failed to start. Make sure the "out" folder is present next to this file.
  pause
)