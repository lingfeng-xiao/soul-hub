# 面小助 - Boss-cli 使用经验

## 2026-03-19 实战经验

### 问题1：Cookie 频繁失效
- **现象**：boss-cli 登录后很快提示 `__zp_stoken__ 已过期`
- **原因**：Boss直聘的 Cookie 有效期短，且多标签页登录会互相顶掉
- **解决方案**：
  1. 用户给 Cookie 后，直接写入 `C:\Users\16343\.config\boss-cli\credential.json`
  2. 写入后立即使用 `boss status` 验证
  3. 准备好最新 Cookie，随时快速更新

### 问题2：batch-greet 需要确认
- **现象**：默认会弹出确认提示 `[y/N]`
- **解决方案**：使用 `echo y | boss batch-greet ...` 自动确认

### 成功流程
1. 搜索远程岗位：`boss search "Java 远程" --city 全国`
2. 批量打招呼：`echo y | boss batch-greet "Java 远程" --city 全国 -n 10`
3. 每批10个，间隔1.5秒防封

### 最佳实践
- **每次用前检查登录状态**：`boss status`
- **发现失效立即更新Cookie**：直接写 JSON 文件到 `~/.config/boss-cli/credential.json`
- **分批打招呼**：每次10个，分批执行，避免被封
- **保存Cookie格式**：
```json
{
  "cookies": {...},
  "saved_at": timestamp
}
```

### 教训
- 不要依赖 boss-cli 的自动登录功能，直接手动更新 Cookie 最稳定
- 用户给 Cookie 时立即使用，不要等
- boss-cli 输出有编码问题（GBK），但不影响实际功能
