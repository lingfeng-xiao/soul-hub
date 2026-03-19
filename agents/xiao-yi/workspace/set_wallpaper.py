#!/usr/bin/env python3
import ctypes
import os

SPI_SETDESKWALLPAPER = 0x0014
result = ctypes.windll.user32.SystemParametersInfoW(SPI_SETDESKWALLPAPER, 0, r"C:\Users\16343\wallpaper_ocean.bmp", 3)
print(f"Result: {result}")
