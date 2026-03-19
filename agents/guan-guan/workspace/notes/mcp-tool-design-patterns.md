# MCP 工具设计模式笔记

## 核心概念
MCP (Model Context Protocol) - AI应用的USB-C标准，用于连接AI到外部系统。

## 三种能力
1. **Resources**: 可读取的数据（类似文件）
2. **Tools**: LLM可调用的函数
3. **Prompts**: 预置提示模板

## 设计模式要点

### 1. STDIO服务器日志
```python
# ❌ 错误 - 会破坏JSON-RPC消息
print("Processing request")

# ✅ 正确 - 写入stderr
print("Processing request", file=sys.stderr)
logging.info("Processing request")
```

### 2. FastMCP工具定义
使用Python类型提示和docstrings自动生成工具定义。

### 3. 错误处理
始终返回有意义的错误信息，便于AI理解和处理。

## 可应用到管管的点
1. **工具设计**：确保工具定义清晰、类型完整
2. **日志规范**：检查现有工具是否有不当输出
3. **扩展思路**：MCP可作为Agent间通信的参考协议
