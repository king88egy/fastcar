Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot "res\mipmap"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$bmp = New-Object System.Drawing.Bitmap 192,192
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::FromArgb(16,19,26))

$carBody = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(224,62,52))
$g.FillRectangle($carBody, 12, 12, 168, 116)

$glass = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(26,36,48))
$g.FillRectangle($glass, 30, 42, 84, 32)
$g.FillRectangle($glass, 30, 82, 84, 30)

$stripe = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,213,79))
$g.FillRectangle($stripe, 30, 78, 84, 6)

$wheel = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$dark = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(15,18,24))
$g.FillEllipse($dark, 30, 118, 32, 52)
$g.FillEllipse($dark, 130, 118, 32, 52)
$g.FillEllipse($wheel, 38, 126, 16, 36)
$g.FillEllipse($wheel, 138, 126, 16, 36)

$fc = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$font = New-Object System.Drawing.Font("Arial", 46, [System.Drawing.FontStyle]::Bold)
$sf = New-Object System.Drawing.StringFormat
$sf.Alignment = [System.Drawing.StringAlignment]::Center
$sf.LineAlignment = [System.Drawing.StringAlignment]::Center
$g.DrawString("FC", $font, $fc, (New-Object System.Drawing.RectangleF(70,14,52,50)), $sf)

$imgPath = Join-Path $dir "ic_launcher.png"
$bmp.Save($imgPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Icon saved: $imgPath"