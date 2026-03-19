# MCP 工具设计模式学习笔记

## 什么是 MCP?
Model Context Protocol - 连接 AI 应用与外部系统的开放标准，类似 AI 领域的 USB-C。

## 三大核心能力
1. **Resources**: 可读取的文件类数据
2. **Tools**: LLM 可调用的函数
3. **Prompts**: 预置提示模板

## OpenClaw 应用启示
OpenClaw 本身就是 MCP 架构的实践：
- 各种工具(feishu_*, browser, exec 等)就是 MCP Tools
- 工具的 schema 定义就是 MCP 的 tool definition
- 消息通道就是 transport layer

## 最佳实践
### STDIO 服务器日志
```python
# ❌ 错误 - 会破坏 JSON-RPC
print("Processing")

# ✅ 正确
print("Processing", file=sys.stderr)
```

### 工具定义
使用 FastMCP + 类型提示 + docstring 自动生成工具定义，减少维护成本。

## 可应用点
1. 未来可考虑 MCP 协议对接更多外部系统
2. 工具日志统一走 stderr，避免 stdout 污染
3. 工具描述使用清晰的 docstring 便于 AI 理解
