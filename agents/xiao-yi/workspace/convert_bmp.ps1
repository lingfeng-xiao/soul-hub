Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile("C:\Users\16343\Downloads\wallpaper_new.png")
$img.Save("C:\Users\16343\Downloads\wallpaper_new.bmp")
$img.Dispose()
Write-Host "Converted to BMP"
