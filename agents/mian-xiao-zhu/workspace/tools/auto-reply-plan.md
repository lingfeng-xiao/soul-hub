# BOSS 自动回复功能实现方案

## 一、需求分析

| 功能 | 优先级 | 难度 |
|------|--------|------|
| 读取聊天记录 | P0 | 中 |
| 发送消息 | P0 | 中 |
| 自动回复 | P1 | 高 |
| 消息提醒 | P2 | 低 |

## 二、技术方案

### 方案 A：基于现有 API 扩展（推荐）

**原理**：复用 boss-cli 现有框架，扩展消息相关 API

```
boss-cli 现有能力:
├── get_friend_list()     # 获取好友列表 ✅ 已实现
├── add_friend()          # 打招呼/投递 ✅ 已实现
└── (缺失)
    ├── get_chat_history()   # 获取聊天记录
    ├── send_message()       # 发送消息
    └── poll_messages()      # 轮询新消息
```

**实现步骤**：

1. **抓包分析消息 API**
   - 使用 Chrome DevTools 或 Charles 抓取 BOSS 网页版 WebSocket 消息
   - 找到聊天记录的 REST API（通常比 WebSocket 更容易逆向）
   - 找到发送消息的 API

2. **新增 API 端点**（预估）
   ```python
   # 聊天记录 API
   CHAT_HISTORY_URL = "/wapi/zpchat/geek/chat/history.json"
   CHAT_LIST_URL = "/wapi/zpchat/geek/chat/list.json"
   
   # 发送消息 API  
   CHAT_SEND_URL = "/wapi/zpchat/geek/chat/send.json"
   ```

3. **新增命令**
   ```bash
   boss chat list              # 查看聊天列表
   boss chat history <boss_id> # 查看与某 Boss 的聊天记录
   boss chat send <boss_id> "你好"  # 发送消息
   boss chat auto              # 自动回复模式（轮询+回复）
   ```

4. **自动回复逻辑**
   ```python
   # 伪代码
   def auto_reply():
       keywords = load_keywords()  # 加载关键词和回复模板
       while True:
           messages = poll_new_messages()
           for msg in messages:
               if msg.is_new:
                   reply = match_keyword(msg.content, keywords)
                   if reply:
                       send_message(msg.boss_id, reply)
           time.sleep(30)  # 每30秒检查一次
   ```

---

### 方案 B：浏览器自动化 (Playwright/Selenium)

**原理**：完全模拟浏览器操作，不逆向 API

**优点**：
- 不怕 API 变化
- 不怕风控更新
- 实现简单

**缺点**：
- 速度慢
- 需要保持浏览器运行
- 容易被检测为机器人

**实现**：
```python
from playwright.sync_api import sync_playwright

def auto_reply_browser():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        page.goto("https://www.zhipin.com/")
        # 登录...
        
        while True:
            # 刷新页面，检查新消息
            page.reload()
            # 点击消息图标
            # 读取最新消息
            # 发送回复
            time.sleep(30)
```

---

### 方案 C：WebSocket 监听（最稳健但最难）

**原理**：逆向 BOSS 的 WebSocket 实时通信协议

- BOSS 使用 wss://im-*.zhipin.com/ 连接
- 需要逆向心跳机制、加密方式
- 复杂度最高

**暂不推荐**，除非方案 A/B 失效

---

## 三、实现计划

### Phase 1：消息读取 (1-2天)
1. 抓包分析聊天记录 API
2. 实现 `boss chat history` 命令
3. 测试并验证

### Phase 2：消息发送 (1-2天)
1. 抓包分析发送消息 API
2. 实现 `boss chat send` 命令
3. 测试并验证

### Phase 3：自动回复 (2-3天)
1. 设计关键词匹配规则
2. 实现轮询机制
3. 添加配置管理
4. 风险控制（避免频繁被封）

---

## 四、风险控制

| 风险 | 应对措施 |
|------|----------|
| API 变化 | 版本检测 + 提示更新 |
| 风控封号 | 降低请求频率，模拟人工操作 |
| 消息漏读 | 增加轮询间隔，记录已读状态 |
| 回复错误 | 支持人工审核模式 |

---

## 五、配置示例

```yaml
# auto-reply.yaml
auto_reply:
  enabled: true
  poll_interval: 30  # 秒
  
  # 关键词回复规则
  keywords:
    - keyword: "招"
      reply: "您好，我对贵公司的职位很感兴趣，请问可以进一步沟通吗？"
    - keyword: "薪资"
      reply: "您好，关于薪资方面我希望进一步了解岗位职责后沟通。"
    - keyword: "面试"
      reply: "好的，我近期都有时间，请问您想安排在什么时候？"
  
  # 休息时段（不自动回复）
  quiet_hours:
    - 22:00 - 08:00
  
  # 每日回复上限
  max_replies_per_day: 50
```

---

## 六、总结

| 方案 | 开发难度 | 稳定性 | 推荐度 |
|------|----------|--------|--------|
| A: API 扩展 | 中 | 中 | ⭐⭐⭐⭐⭐ |
| B: 浏览器自动化 | 低 | 高 | ⭐⭐⭐ |
| C: WebSocket | 高 | 高 | ⭐⭐ |

**推荐先尝试方案 A**，失败再考虑方案 B。

---

_方案制定于 2026-03-19_
