#!/usr/bin/env python3
import os
from PIL import Image

img = Image.open(r"C:\Users\16343\Downloads\wallpaper_space2.jpg")
img = img.resize((1707, 960), Image.Resampling.LANCZOS)
img.save(r"C:\Users\16343\wallpaper_space2.bmp")
print("Converted")
