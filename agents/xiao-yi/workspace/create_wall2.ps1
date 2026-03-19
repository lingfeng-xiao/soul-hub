Add-Type -AssemblyName System.Drawing

$width = 1707
$height = 960

$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)

# 紫色到粉色的渐变
$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point($width, $height)),
    [System.Drawing.Color]::FromArgb(255, 138, 43, 226),
    [System.Drawing.Color]::FromArgb(255, 255, 105, 180)
)

$g.FillRectangle($brush, 0, 0, $width, $height)

# 添加星星
$random = New-Object System.Random
for ($i = 0; $i -lt 80; $i++) {
    $x = $random.Next(0, $width)
    $y = $random.Next(0, $height)
    $size = $random.Next(1, 3)
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200, 255, 255, 255))
    $g.FillEllipse($whiteBrush, $x, $y, $size, $size)
}

$bmp.Save("C:\Users\16343\wallpaper_new.bmp")
$g.Dispose()
$bmp.Dispose()

Write-Host "Wallpaper created: $width x $height"
