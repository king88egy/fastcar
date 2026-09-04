$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$out  = Join-Path $root "out"

New-Item -ItemType Directory -Force -Path $out | Out-Null

$jdk    = "C:\Program Files\Android\Android Studio\jbr"
$sdk    = "C:\Users\ALLAH\AppData\Local\Android\Sdk"
$bt     = Join-Path $sdk "build-tools\36.0.0"
$aj     = Join-Path $sdk "platforms\android-37.0\android.jar"

$javac      = Join-Path $jdk "bin\javac.exe"
$keytool    = Join-Path $jdk "bin\keytool.exe"
$d8         = Join-Path $bt "d8.bat"
$aapt       = Join-Path $bt "aapt.exe"
$zipalign   = Join-Path $bt "zipalign.exe"
$apksigner  = Join-Path $bt "apksigner.bat"
$env:JAVA_HOME = $jdk

$storePass = "fastcar123"
$alias = "fastcar"
$keystore = Join-Path $root "app.keystore"

if (-not (Test-Path $keystore)) {
  Write-Host "[1/6] Generating keystore..."
  & $keytool -genkeypair -v -keystore $keystore -storepass $storePass -keypass $storePass -alias $alias -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Fast Car, OU=Games, O=FastCar, L=Cairo, C=EG" | Out-Null
}

Write-Host "[2/6] Compiling Java sources..."
$clsDir = Join-Path $out "classes"
New-Item -ItemType Directory -Force -Path $clsDir | Out-Null
$sources = Get-ChildItem -Path (Join-Path $root "src") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -source 8 -target 8 -bootclasspath $aj -d $clsDir $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "[3/6] DEX (d8)..."
$dexDir = Join-Path $out "dex"
New-Item -ItemType Directory -Force -Path $dexDir | Out-Null
$classFiles = Get-ChildItem -Path $clsDir -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }
& $d8 --release --min-api 21 --lib $aj --output $dexDir $classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }
$dexFile = Join-Path $dexDir "classes.dex"

Write-Host "[4/6] Packaging resources and assets (aapt)..."
$baseApk = Join-Path $out "base.apk"
Remove-Item -LiteralPath $baseApk -Force -ErrorAction SilentlyContinue
& $aapt package -f -M (Join-Path $root "AndroidManifest.xml") -S (Join-Path $root "res") -A (Join-Path $root "assets") -I $aj -F $baseApk
if ($LASTEXITCODE -ne 0) { throw "aapt failed" }

Write-Host "[5/6] Adding classes.dex + zipalign..."
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($baseApk, 'Update')
$existing = $zip.GetEntry("classes.dex")
if ($existing) { $existing.Delete() }
$entry = $zip.CreateEntry("classes.dex", 'Optimal')
$stream = $entry.Open()
$bytes = [System.IO.File]::ReadAllBytes($dexFile)
$stream.Write($bytes, 0, $bytes.Length)
$stream.Close()
$zip.Dispose()

$aligned = Join-Path $out "aligned.apk"
Remove-Item -LiteralPath $aligned -Force -ErrorAction SilentlyContinue
& $zipalign -f 4 $baseApk $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "[6/6] Signing..."
$final = Join-Path $root "FastCar.apk"
Remove-Item -LiteralPath $final -Force -ErrorAction SilentlyContinue
& $apksigner sign --ks $keystore --ks-pass "pass:$storePass" --key-pass "pass:$storePass" --out $final $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host ""
Write-Host "=== BUILD DONE ==="
Write-Host "APK: $final"
& $apksigner verify --print-certs $final | Select-Object -First 6