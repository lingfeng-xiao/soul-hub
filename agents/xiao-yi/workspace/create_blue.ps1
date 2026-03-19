Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap(1707, 960)
$g = [System.Drawing.Graphics]::FromImage($bmp)

# 蓝色渐变 - 天空/海洋
$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point(1707, 960)),
    [System.Drawing.Color]::FromArgb(255, 0, 150, 255),
    [System.Drawing.Color]::FromArgb(255, 135, 206, 250)
)

$g.FillRectangle($brush, 0, 0, 1707, 960)

# 添加白色云朵
$whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 255, 255, 255))
$g.FillEllipse($whiteBrush, 200, 150, 200, 80)
$g.FillEllipse($whiteBrush, 350, 180, 180, 70)
$g.FillEllipse($whiteBrush, 1200, 300, 250, 100)
$g.FillEllipse($whiteBrush, 800, 600, 300, 120)

$bmp.Save("C:\Users\16343\wallpaper_blue.bmp")
$g.Dispose()
$bmp.Dispose()

Write-Host "Blue sky wallpaper created"
