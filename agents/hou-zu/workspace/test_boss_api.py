# -*- coding: utf-8 -*-
import json
import httpx

with open(r'C:\Users\16343\.config\boss-cli\credential.json', 'r') as f:
    cred = json.load(f)

client = httpx.Client(
    base_url='https://www.zhipin.com',
    cookies=cred['cookies'],
    follow_redirects=True
)

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Referer": "https://www.zhipin.com/web/geek/chat",
}

resp = client.get('/wapi/zprelation/friend/getGeekFriendList.json?page=1&size=20', headers=headers)
data = resp.json()
friends = data.get('zpData', {}).get('result', [])

print('=== 联系人列表 ===')
print(f'联系人数量: {len(friends)}\n')
for i, f in enumerate(friends, 1):
    print(f'{i}. {f.get("name")} ({f.get("brandName", "-")})')
    print(f'   职位: {f.get("jobName", "-")}')
    print(f'   最近消息: {f.get("lastMsg", f.get("lastText", "-"))}')
    print(f'   时间: {f.get("lastMsgTime", "-")}')
    # 打印用于打开聊天的 URL
    sid = f.get('securityId', '')
    if sid:
        print(f'   聊天链接: https://www.zhipin.com/web/geek/chat?friendId={sid[:50]}...')
    print()
