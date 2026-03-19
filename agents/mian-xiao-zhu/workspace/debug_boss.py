import requests

cookies = {
    "__a": "41771477.1710859733.1773855986.1773904771.489.38.2.489",
    "__c": "1773904771",
    "__g": "-",
    "__zp_stoken__": "41a5gS1DDmcONxITDjEE8fT5LPERGQktQSkxIS1B6PEZQS0lFRSptw4%2FDjMKvwpFow5khw4pGOUpGPktQREZNQilKQkZQUElLRsWNw4tBS0bCqE06InjDhsOKwrDClGXDoB97GcK0w5AfQsOGO8O8w5A8w7vDjTo7ERYVFx0Sa20SYV5mFRwUGRlvbBpvYh8gGhkZbxdvDhkmK0LEicOMwqfDgsK7w5DCpMOEw4DDisKow4dGUEtJPUpQwrspRUFGSkpFT8WNxYbFjMWQxYzFjcWGw5zDmMSLw6HFhsWMxZDDi8WNxYbFjMSQw6vFjcWGxYzFkMSGw5A1SsKYw4bCk8KTxLbDoMO7xKbCpsKsxILDjcO7U8ODwqvDvm7CusK4wp96wp1vw5DCtsKZfMKZXVVhw43DjXLDkHVpwr51w4B4w4Zxw4Fsw41cwoJ0eXoSGGQcFEwTw5TCmsOb",
    "bst": "V2RNwgEeL02FhsVtRuyBkfKy-07DrfzSk~|RNwgEeL02FhsVtRuyBkfKy-07DrQxS4~",
    "Hm_lpvt_194df3105ad7148dcf2b98a91b5e727a": "1773907024",
    "HMACCOUNT": "9DC7F26C10851005",
    "lastCity": "101020100",
    "wbg": "0",
    "wt2": "DzyH8aZ0wVigqkEMFNI2T-vjksJi8kIMF2ILxLiB5LtJY72IrEz-A4t-c3nQB4USL5L_7a5o4S2-mJbZd8K1CJw~~",
    "zp_at": "irlf6k7B5-bf1HNIbmPNmRvvgAIVBLGAjvwFVru6cTU~",
}

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
}

url = "https://www.zhipin.com/web/geek/job?query=Java&city=101010000&remoteType=1"
resp = requests.get(url, cookies=cookies, headers=headers, timeout=10)

# 保存HTML看看内容
with open('boss_response.html', 'w', encoding='utf-8') as f:
    f.write(resp.text)

print("HTML saved. Length:", len(resp.text))
print("First 2000 chars:")
print(resp.text[:2000])
