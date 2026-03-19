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

# 获取联系人列表
resp = client.get('/wapi/zprelation/friend/getGeekFriendList.json?page=1&size=1', headers=headers)
friends = resp.json().get('zpData', {}).get('result', [])
friend = friends[0]
security_id = friend.get('securityId', '')

print(f"尝试给 {friend.get('name')} 发送消息...")
print()

# 更多 API 尝试
apis = [
    # 使用不同的参数格式
    ('/wapi/zpchat/geek/chat', {'securityId': security_id, 'msgContent': '没有', 'msgType': 1}),
    ('/wapi/zpchat/geek/chat.json', {'encryptFriendId': security_id, 'content': '没有'}),
    ('/wapi/zpchat/geek/chat.json', {'sid': security_id, 'content': '没有'}),
    
    # 尝试 form 方式
    ('/wapi/zpchat/geek/chat.json', {'securityId': security_id, 'content': '没有'}),
]

for api, data in apis:
    try:
        resp = client.post(api, data=data, headers=headers)
        result = resp.json()
        code = result.get('code', -1)
        msg = result.get('message', '')
        if code != -99:  # 过滤掉无关错误
            print(f"POST {api}: code={code}")
    except Exception as e:
        pass

# 也尝试 GET 方式
print()
print("尝试 GET 方式...")
get_apis = [
    f'/wapi/zpchat/geek/chat.json?securityId={security_id[:30]}&content=没有',
    f'/wapi/zpchat/geek/message.json?securityId={security_id[:30]}&content=没有',
]

for api in get_apis:
    try:
        resp = client.get(api, headers=headers)
        result = resp.json()
        code = result.get('code', -1)
        if code != -99:
            print(f"GET {api[:60]}...: code={code}")
    except:
        pass

print()
print("抱歉，目前未能找到发送聊天消息的 API...")
print("Boss 直聘可能使用 WebSocket 进行实时通讯")
