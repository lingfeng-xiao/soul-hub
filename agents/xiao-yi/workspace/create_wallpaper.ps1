Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap(1920, 1080)
$g = [System.Drawing.Graphics]::FromImage($bmp)

$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point(1920, 1080)),
    [System.Drawing.Color]::FromArgb(255, 138, 43, 226),
    [System.Drawing.Color]::FromArgb(255, 255, 105, 180)
)

$g.FillRectangle($brush, 0, 0, 1920, 1080)

$random = New-Object System.Random
for ($i = 0; $i -lt 100; $i++) {
    $x = $random.Next(0, 1920)
    $y = $random.Next(0, 1080)
    $size = $random.Next(1, 3)
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200, 255, 255, 255))
    $g.FillEllipse($whiteBrush, $x, $y, $size, $size)
}

$bmp.Save("C:\Users\16343\Downloads\wallpaper_new.png")
$g.Dispose()
$bmp.Dispose()

Write-Host "Wallpaper created!"
