import requests
from bs4 import BeautifulSoup

# Cookie from user
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
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Referer": "https://www.zhipin.com/"
}

# 访问BOSS直聘搜索页
url = "https://www.zhipin.com/web/geek/job?query=Java&city=101010000&remoteType=1"

try:
    resp = requests.get(url, cookies=cookies, headers=headers, timeout=10)
    print(f"Status: {resp.status_code}")
    
    soup = BeautifulSoup(resp.text, 'html.parser')
    
    # 找职位列表
    jobs = soup.select('.job-list .job-primary')
    print(f"\n找到 {len(jobs)} 个职位:\n")
    
    for i, job in enumerate(jobs[:10], 1):
        title = job.select_one('.job-title .job-name')
        salary = job.select_one('.job-info .salary')
        company = job.select_one('.company-info .company-name')
        info = job.select_one('.job-info .info-primary')
        
        print(f"{i}. {title.get_text(strip=True) if title else 'N/A'} | {salary.get_text(strip=True) if salary else 'N/A'}")
        print(f"   {company.get_text(strip=True) if company else 'N/A'}")
        print(f"   {info.get_text(strip=True) if info else 'N/A'}")
        print()
        
except Exception as e:
    print(f"Error: {e}")
