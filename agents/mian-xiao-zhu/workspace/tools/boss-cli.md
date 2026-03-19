# Boss CLI (BOSS 直聘命令行工具)

> 面试辅助工具 - 用于快速搜索职位、批量投递、查看推荐等

## 安装

```bash
uv tool install kabi-boss-cli
```

升级：
```bash
uv tool upgrade kabi-boss-cli
```

## 核心命令

| 命令 | 功能 |
|------|------|
| `boss login` | 登录（自动提取浏览器 Cookie） |
| `boss login --cookie-source chrome` | 指定浏览器登录 |
| `boss status` | 查看登录状态 |
| `boss logout` | 退出登录 |
| `boss search "关键词"` | 搜索职位 |
| `boss recommend` | 个性化推荐 |
| `boss show 3` | 按编号查看详情 |
| `boss detail <securityId>` | 查看职位详情 |
| `boss applied` | 已投递职位 |
| `boss interviews` | 面试邀请 |
| `boss chat` | 沟通过的 Boss |
| `boss history` | 浏览历史 |
| `boss greet <securityId>` | 打招呼/投递 |
| `boss batch-greet "关键词" -n 5` | 批量打招呼（前5个） |
| `boss cities` | 支持的城市列表 |
| `boss me` | 个人资料 |

## 搜索筛选参数

```bash
# 城市
boss search "Java" --city 杭州

# 薪资
boss search "Python" --salary 20-30K

# 经验
boss search "后端" --exp 3-5年

# 学历
boss search "AI" --degree 硕士

# 行业
boss search "产品" --industry 互联网

# 公司规模
boss search "数据" --scale 1000-9999人

# 融资阶段
boss search "运维" --stage 已上市

# 职位类型
boss search "实习" --job-type 实习

# 分页
boss search "后端" --city 深圳 -p 2
```

### 薪资选项
`3K以下`, `3-5K`, `5-10K`, `10-15K`, `15-20K`, `20-30K`, `30-50K`, `50K以上`

### 经验选项
`不限`, `在校/应届`, `1年以内`, `1-3年`, `3-5年`, `5-10年`, `10年以上`

### 学历选项
`不限`, `初中及以下`, `中专/中技`, `高中`, `大专`, `本科`, `硕士`, `博士`

### 行业选项
互联网, 电子商务, 游戏, 软件/信息服务, 人工智能, 大数据, 云计算, 区块链, 物联网, 金融, 银行, 保险, 证券/基金, 教育培训, 医疗健康, 房地产, 汽车, 物流/运输, 广告/传媒, 消费品, 制造业, 能源/环保, 政府/非营利, 农业

### 公司规模
`0-20人`, `20-99人`, `100-499人`, `500-999人`, `1000-9999人`, `10000人以上`

### 融资阶段
`不限`, `未融资`, `天使轮`, `A轮`, `B轮`, `C轮`, `D轮及以上`, `已上市`, `不需要融资`

## 输出格式

```bash
# JSON 输出（结构化，便于程序处理）
boss search "Java" --json

# YAML 输出
boss search "Java" --yaml
```

## 批量操作

```bash
# 批量打招呼（自动1.5s防风控延迟）
boss batch-greet "golang" --city 杭州 -n 5

# 预览模式（不实际发送）
boss batch-greet "Python" --salary 20-30K --dry-run

# 导出搜索结果
boss export "Python" -n 50 -o jobs.csv
boss export "golang" --format json -o jobs.json
```

## 支持的城市 (42个)

全国, 北京, 上海, 广州, 深圳, 杭州, 成都, 南京, 武汉, 西安, 苏州, 长沙, 天津, 重庆, 郑州, 东莞, 佛山, 合肥, 青岛, 宁波, 沈阳, 昆明, 大连, 厦门, 珠海, 无锡, 福州, 济南, 哈尔滨, 长春, 南昌, 贵阳, 南宁, 石家庄, 太原, 兰州, 海口, 常州, 温州, 嘉兴, 徐州, 香港

## 常见问题

- **环境异常 (Cookie 过期)**: `boss logout && boss login` 刷新
- **搜索无结果**: 检查城市筛选，使用 `boss cities` 确认支持
- **登录失败**: 尝试 `boss login --qrcode` 二维码登录

## 面小助使用场景

1. **批量找岗**: 根据用户技术栈批量搜索筛选
2. **薪资调研**: 搜索目标城市+岗位了解薪资范围
3. **竞品分析**: 搜索同类型公司了解招聘需求
4. **批量投递**: 使用 batch-greet 批量打招呼（注意1.5s延迟）

---

_更新于 2026-03-18_
