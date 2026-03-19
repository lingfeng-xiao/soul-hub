# Agent 注册表

> ⚠️ 命名必须遵守: `NAMING_CONVENTION.md`

---

## 当前 Agent 列表

| ID | 新名称 | 用途 | 触发关键词 | 状态 |
|----|--------|------|------------|------|
| hou-zu | 后助（后端开发） | 后端开发 | 后端, 代码, 开发, 后助 | ✅ |
| main | Main Agent | 主 Agent | - | ✅ 系统 |
| xiao-yi | 小艺（电脑管家） | UI设计师电脑管家 | 小艺, UI设计师, 电脑管家 | ✅ |
| mian-xiao-zhu | 面小助（面试辅导） | 面试辅导 | 面试, 简历, 找工作, 面小助 | ✅ |
| guan-guan | 管管（管理员） | Agent管理员 | 管管, 创建agent, 管理agent | ✅ |

---

## 目录结构

每个 Agent 只需要维护 `workspace/` 目录下的文件：

```
agents/
├── hou-zu/
│   └── workspace/          ← 定制文件放这里
├── xiao-yi/
│   └── workspace/          ← 定制文件放这里
├── mian-xiao-zhu/
│   └── workspace/          ← 定制文件放这里
└── guan-guan/
    └── workspace/          ← 定制文件放这里
```

---

*最后更新: 2026-03-17*
