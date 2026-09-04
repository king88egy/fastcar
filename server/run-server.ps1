Set-Location -LiteralPath $PSScriptRoot
$studio = "C:\Program Files\Android\Android Studio\jbr\bin"
if (Test-Path "$studio\javac.exe") {
  $javac = "$studio\javac.exe"
  $java = "$studio\java.exe"
} else {
  $javac = (Get-Command javac -ErrorAction Stop).Source
  $java = (Get-Command java -ErrorAction Stop).Source
}
New-Item -ItemType Directory -Force -Path "out" | Out-Null
& $javac -encoding UTF-8 -d out src\com\fastcar\server\ServerMain.java
if (-not $?) { Write-Host "[error] Compilation failed."; exit 1 }
Write-Host "Compilation OK. Starting server. Press Ctrl+C to stop."
& $java -cp out com.fastcar.server.ServerMain $args