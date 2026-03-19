# -*- coding: utf-8 -*-
import os
import glob

desktop = os.path.join(os.environ['USERPROFILE'], 'Desktop')
print("Files on Desktop:")
for f in glob.glob(os.path.join(desktop, '*.docx')):
    print(f)
for f in glob.glob(os.path.join(desktop, '*.pdf')):
    print(f)
