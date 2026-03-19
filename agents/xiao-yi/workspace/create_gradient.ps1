Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap(1707, 960)
$g = [System.Drawing.Graphics]::FromImage($bmp)

$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point(1707, 960)),
    [System.Drawing.Color]::FromArgb(255, 138, 43, 226),
    [System.Drawing.Color]::FromArgb(255, 255, 105, 180)
)

$g.FillRectangle($brush, 0, 0, 1707, 960)

$random = New-Object System.Random
for ($i = 0; $i -lt 80; $i++) {
    $x = $random.Next(0, 1707)
    $y = $random.Next(0, 960)
    $size = $random.Next(1, 3)
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200, 255, 255, 255))
    $g.FillEllipse($whiteBrush, $x, $y, $size, $size)
}

$bmp.Save("C:\Users\16343\wallpaper_gradient.bmp")
$g.Dispose()
$bmp.Dispose()

Write-Host "Gradient wallpaper created"
