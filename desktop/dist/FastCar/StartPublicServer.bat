@echo off
setlocal
set DIR=%~dp0
echo ============================================================
echo FastCar - Public multiplayer mode (no router changes needed)
echo ============================================================
echo.
echo Opening 3 windows:
echo   1) FastCar Server  (this window)
echo   2) Cloudflare Tunnel manager - keep it running
echo   3) Wide URL console - shows your public https link
echo.
echo Once the Tunnel window opens, wait ~30 seconds, then look
echo in the "Public URL" window for https://....trycloudflare.com
echo.
echo Share that https link with your friends - they play from it!
echo The game ON THIS PC connects automatically to the local server.
echo.
echo NOTE: These windows must stay open while friends are playing.
echo ============================================================
echo.
start "FastCar Public Link" cmd /k "set PUBLIC_URL_PREFIX=TryCluster && echo Waiting for tunnel URL... && echo(Copy the https://....trycloudflare.com line from the NEXT window)"
start "Cloudflare Tunnel" "%DIR%cloudflared.exe" tunnel --url http://127.0.0.1:8080
echo FastCar server starting on port 8080 (local players: run FastCar.bat)...
"%DIR%runtime\bin\java.exe" -jar "%DIR%FastCarServer.jar" 8080
pause