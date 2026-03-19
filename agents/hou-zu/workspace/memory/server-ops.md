# 服务器管理知识库

> 持续更新，记录专业的服务器管理最佳实践

---

## 一、安全加固 (Security Hardening)

### 1.1 SSH 安全配置

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| 密码登录 | 禁用 | 只允许公钥登录 |
| Root 登录 | 禁用 | 使用普通用户 sudo |
| 端口 | 22 → 自定义 | 减少暴力扫描 |
| 协议版本 | 2 | 更安全 |
| 空闲超时 | 300s | 自动断开空闲连接 |

```bash
# 推荐 SSH 配置 (/etc/ssh/sshd_config)
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
Port 22022
ClientAliveInterval 300
ClientAliveCountMax 2
```

### 1.2 防火墙配置

```bash
# UFW (Ubuntu)
ufw default deny incoming
ufw default allow outgoing
ufw allow 22022/tcp  # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw enable

# 或使用 iptables
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
iptables -A INPUT -i lo -j ACCEPT
iptables -A INPUT -p tcp --dport 22022 -j ACCEPT
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT
```

### 1.3 自动安全更新

```bash
# Ubuntu
apt install unattended-upgrades
dpkg-reconfigure -plow unattended-upgrades

# 配置文件 /etc/apt/apt.conf.d/50unattended-upgrades
Unattended-Upgrade::Mail "admin@example.com";
Unattended-Upgrade::Automatic-Reboot "true";
```

---

## 二、不可变基础设施 (Immutable Infrastructure)

### 2.1 核心理念

> **"任何变更都通过重新部署完成，环境像 Docker 镜像一样不可变"**

### 2.2 优势

| 优势 | 说明 |
|------|------|
| 快速回滚 | 报错？直接重新部署上一个版本 |
| 灾难恢复 | 另一个地域快速重建整个架构 |
| 版本控制 | 所有变更有记录，可同行评审 |
| 检测漂移 | 自动发现手动修改的配置 |
| 沙盒环境 | 快速创建/销毁测试环境 |

### 2.3 IaC 工具推荐

| 工具 | 特点 |
|------|------|
| **Terraform** | 多云支持，声明式，plan/apply |
| **Ansible** | 幂等配置管理，SSH |
| **CloudFormation** | AWS 原生，JSON/YAML |
| **Pulumi** | 用代码定义基础设施 |

### 2.4 环境分层

```
开发环境 (Dev)     → 完全开放，可随意折腾
暂存环境 (Staging) → 生产镜像，审批部署
生产环境 (Prod)    → 最小权限，只读访问
```

---

## 三、Docker 容器安全

### 3.1 运行原则

- **不以 root 运行容器** - 使用 USER 指令
- **最小化镜像** - 使用 alpine/minimal 基础镜像
- **只读文件系统** - `docker run --read-only`
- **限制资源** - `--memory`, `--cpus`

```dockerfile
# 最佳实践 Dockerfile
FROM node:18-alpine AS builder

# 复制依赖文件先安装
COPY package*.json ./
RUN npm ci --only=production

# 运行用户
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /bin/sh -D appuser
USER appuser

COPY --from=builder /node_modules ./node_modules
COPY . .
```

### 3.2 Docker 安全命令

```bash
# 扫描漏洞
docker scan myimage:latest

# 限制容器权限
docker run --read-only --tmpfs /tmp myimage

# 禁止特权模式
docker run --privileged=false myimage

# 网络隔离
docker network create internal
docker run --network internal myimage
```

---

## 四、监控与告警

### 4.1 核心指标 (RED 方法)

| 指标 | 说明 |
|------|------|
| **Rate** | 请求率 (QPS) |
| **Errors** | 错误率 |
| **Duration** | 延迟 (P50/P95/P99) |

### 4.2 基础设施指标 (USE 方法)

| 指标 | 说明 |
|------|------|
| **Utilization** | 利用率 (CPU/内存/磁盘) |
| **Saturation** | 饱和度 (负载队列) |
| **Errors** | 错误数 |

### 4.2 监控工具栈

```
数据采集 → Prometheus / Node Exporter
可视化   → Grafana
告警     → Alertmanager → 钉钉/邮件/Slack
日志     → ELK / Loki + Promtail
```

### 4.3 基础监控命令

```bash
# 系统资源
top -b -n 1           # CPU/内存实时
htop                   # 交互式 (如安装)
free -h               # 内存
df -h                 # 磁盘
iostat -x 1 3         # IO 状态
netstat -tuln         # 监听端口

# 进程
ps aux --sort=-%cpu   # 按CPU排序
ps aux --sort=-%mem   # 按内存排序
lsof -i :8080         # 端口占用

# Docker
docker stats          # 容器资源
docker logs -f container
docker inspect container
```

---

## 五、备份与恢复

### 5.1 备份策略 3-2-1 原则

> **3 份副本 + 2 种介质 + 1 份异地**

### 5.2 备份内容

| 类型 | 方式 | 频率 |
|------|------|------|
| 数据库 | mysqldump / pg_dump | 每日 |
| 文件 | rsync / rclone | 每日 |
| 快照 | 云盘快照 | 每周 |
| 配置 | Git 版本控制 | 每次变更 |

### 5.3 恢复演练

> **备份不测试 = 没备份**

- 每季度进行恢复演练
- 记录 RTO (恢复时间目标) 和 RPO (恢复点目标)

---

## 六、性能优化

### 6.1 Linux 性能调优

```bash
# 内核参数调优 (/etc/sysctl.conf)
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.ip_local_port_range = 1024 65535
net.ipv4.tcp_fin_timeout = 30
vm.swappiness = 10

# 文件描述符
ulimit -n 65535
```

### 6.2 Nginx 优化

```nginx
worker_processes auto;
worker_connections 65535;
keepalive_timeout 65;
gzip on;
gzip_types text/plain application/json application/javascript;

# 静态资源缓存
location ~* \.(jpg|png|css|js)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

### 6.3 Redis 优化

```redis
# 内存管理
maxmemory 2gb
maxmemory-policy allkeys-lru

# 持久化
save 900 1
save 300 10
save 60 10000

# 连接
timeout 300
tcp-keepalive 60
```

---

## 七、日常运维清单

### 7.1 每日检查

- [ ] 服务是否正常运行 (curl health endpoint)
- [ ] 磁盘空间是否充足 (< 80%)
- [ ] 内存使用是否正常 (< 90%)
- [ ] 错误日志是否有异常增长

### 7.2 每周检查

- [ ] 安全更新是否安装
- [ ] 备份是否成功完成
- [ ] 证书是否即将过期 (< 30天)
- [ ] 日志文件是否需要轮转

### 7.3 每月检查

- [ ] 性能趋势分析
- [ ] 容量规划
- [ ] 灾难恢复测试
- [ ] 安全漏洞扫描

---

## 八、紧急故障处理

### 8.1 快速诊断流程

```
1. 检查服务状态    → systemctl status nginx
2. 检查端口监听    → netstat -tuln | grep 80
3. 检查进程资源    → top / htop
4. 检查日志        → journalctl -u nginx -n 50
5. 检查网络连接    → ping / curl localhost
6. 检查防火墙    → iptables -L -n
```

### 8.2 常见问题快速处理

| 问题 | 命令 |
|------|------|
| 端口被占 | `lsof -i :8080` → `kill -9 PID` |
| 磁盘满 | `du -sh /*` 找到大目录 |
| 内存满 | `free -h` → `sync && echo 3 > /proc/sys/vm/drop_caches` |
| SSH 无法登录 | 通过 VNC/控制台检查 |

---

## 九、常用服务器管理命令

### 9.1 系统信息

```bash
# 基础信息
uname -a              # 内核信息
cat /etc/os-release   # 系统版本
uptime                # 运行时间/负载
whoami                # 当前用户

# 资源
free -h               # 内存
df -h                 # 磁盘
top -b -n 1           # 实时状态
```

### 9.2 服务管理

```bash
# systemd
systemctl status nginx
systemctl restart nginx
systemctl enable nginx   # 开机启动
journalctl -u nginx -f   # 查看日志

# Docker
docker ps -a
docker logs -f container
docker-compose up -d
docker-compose logs -f
```

### 9.3 网络

```bash
# 网络状态
ip addr
ip route
netstat -tuln
ss -s

# 连通性
ping -c 3 example.com
curl -v http://localhost:8080/health
```

---

*最后更新: 2026-03-19*
*来源: DigitalOcean, Docker Docs, 腾讯云, 个人实践*
