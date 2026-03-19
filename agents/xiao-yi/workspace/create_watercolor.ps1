Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap(1707, 960)
$g = [System.Drawing.Graphics]::FromImage($bmp)

$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point(1707, 960)),
    [System.Drawing.Color]::FromArgb(255, 147, 112, 219),
    [System.Drawing.Color]::FromArgb(255, 255, 182, 193)
)

$g.FillRectangle($brush, 0, 0, 1707, 960)

$random = New-Object System.Random

# 底层水彩纹理
for ($i = 0; $i -lt 20; $i++) {
    $x = $random.Next(0, 1707)
    $y = $random.Next(0, 960)
    $w = $random.Next(200, 500)
    $h = $random.Next(150, 400)
    $alpha = $random.Next(20, 50)
    $color = [System.Drawing.Color]::FromArgb($alpha, 255, 255, 255)
    $brush2 = New-Object System.Drawing.SolidBrush($color)
    $g.FillEllipse($brush2, $x, $y, $w, $h)
}

# 中层彩色光斑
$color1 = [System.Drawing.Color]::FromArgb(30, 255, 192, 203)
$color2 = [System.Drawing.Color]::FromArgb(30, 173, 216, 230)
$color3 = [System.Drawing.Color]::FromArgb(30, 221, 160, 246)

for ($i = 0; $i -lt 30; $i++) {
    $x = $random.Next(0, 1707)
    $y = $random.Next(0, 960)
    $w = $random.Next(100, 300)
    $h = $random.Next(80, 250)
    if ($i % 3 -eq 0) { $g.FillEllipse($color1, $x, $y, $w, $h) }
    elseif ($i % 3 -eq 1) { $g.FillEllipse($color2, $x, $y, $w, $h) }
    else { $g.FillEllipse($color3, $x, $y, $w, $h) }
}

# 顶层星光
for ($i = 0; $i -lt 100; $i++) {
    $x = $random.Next(0, 1707)
    $y = $random.Next(0, 960)
    $size = $random.Next(1, 2)
    $alpha = $random.Next(100, 200)
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($alpha, 255, 255, 255))
    $g.FillEllipse($whiteBrush, $x, $y, $size, $size)
}

$bmp.Save("C:\Users\16343\wallpaper_watercolor.bmp")
$g.Dispose()
$bmp.Dispose()

Write-Host "Watercolor style created"
