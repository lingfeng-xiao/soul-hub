Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile("C:\Users\16343\Downloads\wallpaper_water.jpg")
$width = 1707
$height = 960
$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.DrawImage($img, 0, 0, $width, $height)
$bmp.Save("C:\Users\16343\wallpaper_water.bmp")
$g.Dispose()
$bmp.Dispose()
$img.Dispose()
Write-Host "Done"
