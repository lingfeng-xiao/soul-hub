# -*- coding: utf-8 -*-
"""
Boss 直聘自动化工具 - 使用 Playwright
需要先安装: pip install playwright && playwright install chromium
"""
import asyncio
from playwright.async_api import async_playwright
import json

async def main():
    # 读取 cookie
    with open(r'C:\Users\16343\.config\boss-cli\credential.json', 'r') as f:
        cred = json.load(f)
    
    cookies = cred.get('cookies', {})
    
    async with async_playwright() as p:
        # 启动浏览器
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context()
        
        # 添加 cookie
        for name, value in cookies.items():
            await context.add_cookies([{
                'name': name,
                'value': value,
                'domain': '.zhipin.com',
                'path': '/'
            }])
        
        page = await context.new_page()
        
        # 打开聊天页面
        await page.goto('https://www.zhipin.com/web/geek/chat')
        await page.wait_for_load_state('networkidle')
        
        print("页面加载完成!")
        
        # 等待用户手动操作
        # 使用 aria-label 定位第一个联系人
        try:
            # 尝试找到第一个聊天项目并点击
            chat_item = page.locator('[class*="chat-item"], [class*="friend-item"]').first
            await chat_item.click()
            print("已点击第一个联系人")
        except Exception as e:
            print(f"自动点击失败: {e}")
            print("请手动点击一个联系人")
        
        # 等待用户确认
        input("打开聊天窗口后，按回车继续...")
        
        # 找到输入框并输入内容
        try:
            # 尝试多种输入框定位方式
            textbox = page.locator('textarea[placeholder*="发送"], input[placeholder*="发送"], [contenteditable="true"]').first
            await textbox.fill('没有')
            print("已输入: 没有")
            
            # 点击发送按钮
            send_btn = page.locator('button:has-text("发送"), [class*="send-btn"]').first
            await send_btn.click()
            print("已发送!")
        except Exception as e:
            print(f"发送失败: {e}")
            print("请手动发送")
        
        await asyncio.sleep(2)
        await browser.close()

if __name__ == "__main__":
    asyncio.run(main())
