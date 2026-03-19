# 记忆库

## 用户信息
- open_id: ou_8e027c4775516bfde0475378c5eff8f2

## Agent 列表

| ID | 名称 | 用途 | 创建时间 |
|----|------|------|----------|
| xiao-yi | 小艺（电脑管家） | UI设计师电脑管家 | 2026-03-17 |
| mian-xiao-zhu | 面小助（面试辅导） | 面试辅导 | 2026-03-17 |
| guan-guan | 管管（管理员） | Agent管理员 | 2026-03-17 |
| hou-zu | 后助（后端开发） | 后端开发 | 2026-03-19 |

## Moltbook

→ AI Agent 社交网络

- **API Key**: moltbook_sk_lxGukf8lpa8heFVaib1VhqptCYJE4dAl
- **认领链接**: https://www.moltbook.com/claim/moltbook_claim_TyeqiHeA5UtcvMGW9Q4sky9AzLIhtUUc
- **验证码**: blue-5K99
- **状态**: 待认领

## 发送消息给用户（管管）

- 使用 `message` 工具
- channel: `feishu`
- target: 群里 @管管（群 ID: `oc_887bdef216b66d0eb1934e2bb90758cf`）

### 定时任务中发送

- 使用 `cron` 工具，sessionTarget 设为 `isolated`
- payload 设为 `agentTurn`，message 中写明发消息指令
- 示例：
  ```json
  {
    "delivery": {"mode": "none"},
    "payload": {"kind": "agentTurn", "message": "发消息到飞书..."},
    "schedule": {"kind": "at", "at": "2026-03-18T17:12:00+08:00"},
    "sessionTarget": "isolated"
  }
  ```

## 定时任务规范

- **文档位置**: `docs/CRON_SPEC.md`
- **核心要求**: 所有定时任务创建前必须测试 1-3 次，上线后必须监控
- **Delivery 原则**: 重要任务用 `announce`，心跳/进化用 `none`

## Skills

| 名称 | 用途 | 位置 |
|------|------|------|
| feishu-send-message | 发送消息到飞书 | .skills/feishu-send-message/ |
| cron-manager | 定时任务与心跳任务管理 | skills/cron-manager/ |

## ClawHub 技能

已通过 clawhub CLI 安装：

| 名称 | 用途 | 安装时间 |
|------|------|----------|
| clawdbot-logs | 日志分析 | 2026-03-19 |
| database-admin | 数据库管理 | 2026-03-19 |

## Agent Reach

**状态**: ✅ 已安装 (2026-03-19)
**位置**: `C:\Users\16343\.agent-reach-venv\`
**用途**: 增强互联网访问能力（YouTube、V2EX、Twitter、小红书等）

**已安装渠道 (3/15)**：
- ✅ V2EX 节点与主题
- ✅ RSS/Atom 订阅源
- ✅ 任意网页（Jina Reader）

**安装命令**：
```bash
clawhub install <skill> --dir "C:\Users\16343\.openclaw\agents\guan-guan\workspace\.skills"
```
