# -*- coding: utf-8 -*-
import os
import glob
import zipfile
import re

desktop = os.path.join(os.environ['USERPROFILE'], 'Desktop')

# Find all resume files
docx_files = glob.glob(os.path.join(desktop, '*.docx'))

for docx_file in docx_files:
    print(f"\n=== Reading: {docx_file} ===\n")
    try:
        with zipfile.ZipFile(docx_file, 'r') as z:
            content = z.read('word/document.xml').decode('utf-8')
            # Remove XML tags
            text = re.sub(r'<[^>]+>', ' ', content)
            # Clean up whitespace
            text = re.sub(r'\s+', ' ', text)
            # Decode HTML entities
            text = text.replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&').replace('&apos;', "'").replace('&quot;', '"')
            print(text)
    except Exception as e:
        print(f"Error reading {docx_file}: {e}")
