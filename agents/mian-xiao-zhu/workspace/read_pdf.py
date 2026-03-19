# -*- coding: utf-8 -*-
import os
import glob
import PyPDF2
import sys

# Set UTF-8 encoding
sys.stdout.reconfigure(encoding='utf-8')

desktop = os.path.join(os.environ['USERPROFILE'], 'Desktop')

# Find PDF files
for root, dirs, files in os.walk(desktop):
    for filename in files:
        if '简历' in filename or 'java' in filename.lower():
            pdf_file = os.path.join(root, filename)
            print(f"\n=== Reading: {pdf_file} ===\n")
            try:
                with open(pdf_file, 'rb') as f:
                    reader = PyPDF2.PdfReader(f)
                    for i, page in enumerate(reader.pages):
                        print(f"--- Page {i+1} ---")
                        text = page.extract_text()
                        print(text)
            except Exception as e:
                print(f"Error reading {pdf_file}: {e}")
