# -*- coding: utf-8 -*-
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

import json
import httpx

with open(r'C:\Users\16343\.config\boss-cli\credential.json', 'r') as f:
    cred = json.load(f)

client = httpx.Client(
    base_url='https://www.zhipin.com',
    cookies=cred['cookies'],
    follow_redirects=True
)

headers = {'Referer': 'https://www.zhipin.com/web/geek/chat'}

resp = client.get(
    '/wapi/zprelation/friend/getGeekFriendList.json?page=1&size=20',
    headers=headers
)

friends = resp.json().get('zpData', {}).get('result', [])

print('=== 联系人列表 ===')
print()

for i, f in enumerate(friends, 1):
    name = f.get('name', '未知')
    brand = f.get('brandName', '-')
    msg = f.get('lastMsg', '-')
    
    print(f'{i}. {name} ({brand})')
    print(f'   最新消息: {msg}')
    print()
