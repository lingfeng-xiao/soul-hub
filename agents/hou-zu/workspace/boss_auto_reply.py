# -*- coding: utf-8 -*-
"""
Boss 直聘自动回复助手
功能：自动获取消息并使用 AI 回复
"""

import json
import time
import random
import httpx
from typing import List, Dict, Optional

# ============ 配置部分 ============
BOSS_COOKIE_PATH = r"C:\Users\16343\.config\boss-cli\credential.json"

# 安全限制
MIN_REPLY_INTERVAL = 300  # 最小回复间隔 5 分钟
MAX_DAILY_REPLIES = 50   # 每天最多回复 50 条
SAFE_HOURS = range(8, 22)  # 只在 8:00-22:00 自动回复


class BossAutoReply:
    def __init__(self):
        self.cookies = self._load_cookies()
        self.client = httpx.Client(
            base_url='https://www.zhipin.com',
            cookies=self.cookies,
            follow_redirects=True,
            timeout=30.0
        )
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Referer": "https://www.zhipin.com/web/geek/chat",
        }
        self.today_count = 0
        
    def _load_cookies(self) -> dict:
        """加载 Cookie"""
        with open(BOSS_COOKIE_PATH, 'r') as f:
            cred = json.load(f)
        return cred.get('cookies', {})
    
    def get_friend_list(self) -> List[Dict]:
        """获取联系人列表"""
        resp = self.client.get(
            '/wapi/zprelation/friend/getGeekFriendList.json?page=1&size=20',
            headers=self.headers
        )
        data = resp.json()
        return data.get('zpData', {}).get('result', [])
    
    def can_reply(self) -> bool:
        """检查是否可以回复"""
        from datetime import datetime
        now = datetime.now()
        
        # 检查时间
        if now.hour not in SAFE_HOURS:
            print(f"❌ 当前时间 {now.hour} 点，不在自动回复时间范围内 (8:00-22:00)")
            return False
            
        # 检查次数
        if self.today_count >= MAX_DAILY_REPLIES:
            print(f"❌ 今日已回复 {self.today_count} 条，达到上限 {MAX_DAILY_REPLIES}")
            return False
            
        return True
    
    def generate_reply(self, message: str, boss_name: str = "") -> str:
        """使用 AI 生成回复（这里用简单规则，实际可以接入 AI API）"""
        # 简单的回复模板，实际使用时可以接入 Kimi/ChatGPT 等
        replies = [
            f"您好，感谢您的联系！我很有兴趣和您详细沟通一下这个职位。",
            f"谢谢您的邀请，请问可以详细了解一下岗位职责吗？",
            f"您好，我对您发布的职位很感兴趣，请问后续流程是怎样的？",
            f"感谢您的消息，我方便了解一下薪资范围吗？",
        ]
        
        # 根据消息关键词选择回复
        if "经验" in message or "熟悉" in message:
            return f"您好，我有一定相关经验，可以深入交流一下。"
        elif "薪资" in message or "工资" in message:
            return f"谢谢关心，期待与您沟通具体的薪资待遇。"
        elif "面试" in message:
            return f"好的，我时间灵活，可以根据您的安排来。"
        else:
            return random.choice(replies)
    
    def send_reply(self, security_id: str, message: str) -> bool:
        """发送回复消息"""
        # 添加随机延迟，模拟人类行为
        time.sleep(random.uniform(2, 5))
        
        # 调用打招呼 API（实际上也可以用来发送消息）
        # 注意：boss-cli 的 add_friend 实际上是打招呼
        # 真正的聊天消息 API 需要进一步探索
        
        # 这里暂时返回 False，因为发送消息 API 还需要研究
        print(f"⚠️ 发送消息功能待开发: {message[:20]}...")
        return False
    
    def run_once(self):
        """运行一次检查和回复"""
        print("\n" + "="*50)
        print("🔍 检查新消息...")
        
        if not self.can_reply():
            return
            
        friends = self.get_friend_list()
        print(f"📋 共有 {len(friends)} 个联系人")
        
        for friend in friends:
            name = friend.get('name', '未知')
            brand = friend.get('brandName', '未知公司')
            last_msg = friend.get('lastMsg', '')
            
            print(f"\n👤 {name} ({brand})")
            print(f"   最后消息: {last_msg}")
            
            # 这里可以添加逻辑判断是否需要回复
            # 比如：只回复未读消息、只回复特定关键词等
            
        print("\n✅ 检查完成")
        self.today_count += 1


def main():
    """主函数"""
    # 设置 UTF-8 编码
    import sys
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    
    bot = BossAutoReply()
    
    # 手动触发一次
    bot.run_once()
    
    # 后续可以加入定时任务循环
    # while True:
    #     bot.run_once()
    #     time.sleep(MIN_REPLY_INTERVAL)


if __name__ == "__main__":
    main()
