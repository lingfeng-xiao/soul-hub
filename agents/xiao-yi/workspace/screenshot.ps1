Add-Type -AssemblyName System.Windows.Forms
$bmp = New-Object System.Drawing.Bitmap(1707, 960)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen(0, 0, 0, 0, (New-Object System.Drawing.Size(1707, 960)))
$bmp.Save('C:\Users\16343\.openclaw\agents\xiao-yi\workspace\perception_data\desktop.png')
$g.Dispose()
$bmp.Dispose()
Write-Host "Screenshot saved"
