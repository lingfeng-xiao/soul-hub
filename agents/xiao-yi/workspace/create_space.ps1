Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap(1707, 960)
$g = [System.Drawing.Graphics]::FromImage($bmp)

$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point(1707, 960)),
    [System.Drawing.Color]::FromArgb(255, 10, 10, 50),
    [System.Drawing.Color]::FromArgb(255, 0, 0, 0)
)

$g.FillRectangle($brush, 0, 0, 1707, 960)

$random = New-Object System.Random

# 白色星星
for ($i = 0; $i -lt 150; $i++) {
    $x = $random.Next(0, 1707)
    $y = $random.Next(0, 960)
    $size = $random.Next(1, 3)
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200, 255, 255, 255))
    $g.FillEllipse($whiteBrush, $x, $y, $size, $size)
}

# 彩色星星
$yellowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 200, 100))
$blueBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 100, 200, 255))
$pinkBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 150, 200))

for ($i = 0; $i -lt 10; $i++) {
    $g.FillEllipse($yellowBrush, $random.Next(0, 1707), $random.Next(0, 960), 3, 3)
}
for ($i = 0; $i -lt 10; $i++) {
    $g.FillEllipse($blueBrush, $random.Next(0, 1707), $random.Next(0, 960), 3, 3)
}
for ($i = 0; $i -lt 10; $i++) {
    $g.FillEllipse($pinkBrush, $random.Next(0, 1707), $random.Next(0, 960), 3, 3)
}

$bmp.Save("C:\Users\16343\wallpaper_space.bmp")
$g.Dispose()
$bmp.Dispose()

Write-Host "Space wallpaper created"
