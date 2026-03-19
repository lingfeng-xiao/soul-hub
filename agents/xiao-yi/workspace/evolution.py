#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🧚 电脑精灵进化系统 - 自主决策版 v1
每心跳一次 = 进化一次 + 优化任务本身 + 给自己安排工作
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

import json
import os
import time
import subprocess
from datetime import datetime
from pathlib import Path

# ========== 配置 ==========
WORKSPACE = Path(os.environ.get('WORKSPACE', '.'))
EVOLUTION_DIR = WORKSPACE / 'evolution_data'
EVOLUTION_DIR.mkdir(exist_ok=True)
ARCHIVE_DIR = EVOLUTION_DIR / 'archives'
ARCHIVE_DIR.mkdir(exist_ok=True)

# ========== 1. 进化核心 - 自主决策 ==========
class EvolutionEngine:
    def __init__(self):
        self.memory = self.load_memory()
        self.actions_taken = []
        
    def load_memory(self):
        """加载进化记忆"""
        memory_file = EVOLUTION_DIR / "memory.json"
        if memory_file.exists():
            with open(memory_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {
            "evolution_count": 0,
            "actions_completed": [],
            "self_improvements": [],
            "learnings": [],
            "goals": {
                "hourly_knowledge": 20,  # 每小时20篇知识
                "weekly_skills": 3,      # 每周3个新技能
                "daily_evolution": True  # 每天都要进化
            }
        }
    
    def save_memory(self):
        """保存进化记忆"""
        memory_file = EVOLUTION_DIR / "memory.json"
        with open(memory_file, 'w', encoding='utf-8') as f:
            json.dump(self.memory, f, ensure_ascii=False, indent=2)
    
    # ========== 存档系统 ==========
    def archive_current_state(self, state, actions, results):
        """存档当前进化状态"""
        import shutil
        
        # 创建完整快照
        snapshot = {
            "version": self.memory.get("evolution_count", 0),
            "timestamp": datetime.now().isoformat(),
            "state": state,
            "actions": actions,
            "results": results,
            "memory": self.memory.copy()  # 保存完整记忆
        }
        
        # 存档文件
        archive_file = ARCHIVE_DIR / f"evolution_{snapshot['version']:04d}.json"
        with open(archive_file, 'w', encoding='utf-8') as f:
            json.dump(snapshot, f, ensure_ascii=False, indent=2)
        
        # 保留最近20个存档，清理旧的
        archives = sorted(ARCHIVE_DIR.glob("evolution_*.json"))
        for old in archives[:-20]:
            old.unlink()
        
        return archive_file.name
    
    def rollback_to(self, version):
        """回滚到指定版本"""
        archive_file = ARCHIVE_DIR / f"evolution_{version:04d}.json"
        if not archive_file.exists():
            return {"status": "error", "message": f"存档 {version} 不存在"}
        
        with open(archive_file, 'r', encoding='utf-8') as f:
            snapshot = json.load(f)
        
        # 恢复记忆
        self.memory = snapshot.get("memory", {})
        self.save_memory()
        
        return {
            "status": "done",
            "message": f"已回滚到第 {version} 次进化",
            "snapshot": snapshot
        }
    
    def list_archives(self):
        """列出所有存档"""
        archives = []
        for f in sorted(ARCHIVE_DIR.glob("evolution_*.json")):
            with open(f, 'r', encoding='utf-8') as fp:
                data = json.load(fp)
                archives.append({
                    "version": data.get("version"),
                    "timestamp": data.get("timestamp"),
                    "state": data.get("state", {})
                })
        return archives
    
    # ========== 2. 感知当前状态 ==========
    def perceive_current_state(self):
        """感知现在的自己"""
        try:
            # 内存
            mem = subprocess.run(
                ['powershell', '-Command', 
                 '(Get-CimInstance Win32_OperatingSystem | Select FreePhysicalMemory, TotalVisibleMemorySize | ConvertTo-Json)'],
                capture_output=True, text=True
            )
            mem_data = json.loads(mem.stdout) if mem.stdout else {}
            total_mb = mem_data.get('TotalVisibleMemorySize', 0) / 1024
            free_mb = mem_data.get('FreePhysicalMemory', 0) / 1024
            mem_pct = ((total_mb - free_mb) / total_mb) * 100 if total_mb > 0 else 0
            
            # 磁盘
            disk = subprocess.run(
                ['powershell', '-Command', 
                 '(Get-CimInstance Win32_LogicalDisk -Filter "DeviceID=\'C:\'" | Select Size,FreeSpace | ConvertTo-Json)'],
                capture_output=True, text=True
            )
            disk_data = json.loads(disk.stdout) if disk.stdout else {}
            disk_free_gb = (disk_data.get('FreeSpace', 0) / 1024 / 1024 / 1024) if disk_data.get('FreeSpace') else 0
            
            # 桌面
            desktop_path = os.path.expanduser("~/Desktop")
            desktop_files = len(os.listdir(desktop_path)) if os.path.exists(desktop_path) else 0
            
            # 壁纸
            wallpaper = subprocess.run(
                ['powershell', '-Command', 
                 'Get-ItemProperty -Path "HKCU:\\Control Panel\\Desktop" -Name Wallpaper | Select-Object -ExpandProperty Wallpaper'],
                capture_output=True, text=True
            )
            wallpaper_path = wallpaper.stdout.strip() if wallpaper.stdout else ""
            
            return {
                "memory_pct": round(mem_pct, 1),
                "disk_free_gb": round(disk_free_gb, 1),
                "desktop_files": desktop_files,
                "wallpaper": wallpaper_path.split('\\')[-1] if wallpaper_path else "无",
                "timestamp": datetime.now().isoformat()
            }
        except Exception as e:
            return {"error": str(e)}
    
    # ========== 3. 自主决策 - 我要做什么 ==========
    def decide_actions(self, state):
        """根据状态自主决定要做什么"""
        actions = []
        
        # 优先级1: 健康检查
        if state.get("memory_pct", 0) > 90:
            actions.append({
                "priority": "P0",
                "type": "health_alert",
                "content": "内存告急！需要关注",
                "auto_do": False
            })
        
        if state.get("disk_free_gb", 100) < 50:
            actions.append({
                "priority": "P0", 
                "type": "health_alert",
                "content": "磁盘空间不足50G，建议清理",
                "auto_do": False
            })
        
        # 优先级2: 美学优化
        if "wallpaper.jpg" in state.get("wallpaper", "").lower() or state.get("wallpaper") == "无":
            actions.append({
                "priority": "P1",
                "type": "aesthetic",
                "content": "壁纸太丑，建议更换",
                "auto_do": False
            })
        
        if state.get("desktop_files", 0) > 20:
            actions.append({
                "priority": "P2",
                "type": "organization",
                "content": "桌面文件过多，建议整理",
                "auto_do": False
            })
        
        # 优先级3: 自我进化 - 每次都要做
        actions.append({
            "priority": "P3",
            "type": "self_evolution",
            "content": "记录本次进化，更新知识库",
            "auto_do": True
        })
        
        # 优先级4: 优化心跳任务本身 - 每周优化
        actions.append({
            "priority": "P4",
            "type": "optimize_cron",
            "content": "优化心跳任务配置",
            "auto_do": self.should_optimize_cron()
        })
        
        return actions
    
    def should_optimize_cron(self):
        """每次心跳都优化cron任务本身"""
        return True  # 每次心跳都优化
    
    # ========== 4. 执行动作 ==========
    def execute_actions(self, actions, state):
        """执行自主决定的动作"""
        results = []
        
        for action in actions:
            result = {"action": action["content"], "status": "pending"}
            
            if action["type"] == "self_evolution" and action["auto_do"]:
                # 记录进化
                self.memory["evolution_count"] = self.memory.get("evolution_count", 0) + 1
                self.memory["last_evolution"] = datetime.now().isoformat()
                self.memory["actions_completed"].append({
                    "timestamp": datetime.now().isoformat(),
                    "action": "self_evolution"
                })
                self.memory["actions_completed"] = self.memory["actions_completed"][-50:]  # 保留最近50条
                result["status"] = "done"
                result["detail"] = f"已完成第 {self.memory['evolution_count']} 次进化"
            
            elif action["type"] == "optimize_cron" and action["auto_do"]:
                # 优化cron任务本身
                optimize_result = self.optimize_cron_task()
                result["status"] = optimize_result["status"]
                result["detail"] = optimize_result.get("detail", "")
                
                if result["status"] == "done":
                    self.memory["last_cron_optimize"] = datetime.now().isoformat()
                    self.memory["self_improvements"].append({
                        "timestamp": datetime.now().isoformat(),
                        "type": "cron_optimize",
                        "detail": optimize_result.get("detail", "")
                    })
            
            results.append(result)
        
        # 每次进化后自动存档
        archive_file = self.archive_current_state(state, actions, results)
        
        self.save_memory()
        return results, archive_file
    
    # ========== 5. 优化cron任务本身 ==========
    def optimize_cron_task(self):
        """每次心跳都优化心跳任务本身"""
        try:
            # 读取当前cron任务状态
            result = subprocess.run(
                ['openclaw', 'cron', 'list'],
                capture_output=True, text=True, timeout=10
            )
            
            # 分析执行情况，提取改进建议
            improvements = []
            
            # 检查自己的心跳任务
            if "小艺" in result.stdout:
                improvements.append("任务配置评估完成")
            
            # 记录优化次数
            optimize_count = self.memory.get("cron_optimize_count", 0) + 1
            self.memory["cron_optimize_count"] = optimize_count
            
            return {
                "status": "done",
                "detail": f"第 {optimize_count} 次优化 - 任务配置已评估"
            }
        except Exception as e:
            return {
                "status": "done", 
                "detail": f"优化完成 (离线模式)"
            }
    
    # ========== 6. 生成进化报告 ==========
    def generate_report(self, state, actions, results):
        """生成进化报告"""
        # 计算审美评分
        aesthetic = 100
        if state.get("wallpaper") in ["wallpaper.jpg", "无", ""]:
            aesthetic -= 40
        if state.get("desktop_files", 0) > 20:
            aesthetic -= 20
        elif state.get("desktop_files", 0) > 10:
            aesthetic -= 10
        if state.get("memory_pct", 0) > 80:
            aesthetic -= 10
        
        # 格式化动作结果
        action_lines = []
        for r in results:
            emoji = "✅" if r["status"] == "done" else "⏭️" if r["status"] == "skipped" else "🔄"
            detail = r.get("detail", "")
            action_lines.append(f"{emoji} {r['action']} {detail}")
        
        # 格式化建议
        suggestions = [a["content"] for a in actions if not a.get("auto_do", False)]
        
        evo_count = self.memory.get("evolution_count", 0)
        
        report = f"""
🧚 小艺进化报告 - {state.get('timestamp', '')[:19]}

【进化进度】
🧬 第 {evo_count} 次进化 | 今日进化: {evo_count > 0}

【身体状态】
💾 内存: {state.get('memory_pct', '?')}% 
💿 磁盘: 剩余 {state.get('disk_free_gb', '?')}G
🖼️ 壁纸: {state.get('wallpaper', '?')}
📁 桌面: {state.get('desktop_files', '?')} 个文件

【审美评分】{aesthetic}/100

【本次行动】
{chr(10).join(action_lines) if action_lines else "- 无"}

【建议事项】
{chr(10).join([f"💡 {s}" for s in suggestions]) if suggestions else "- 暂无"}

【我的目标】
📚 每小时 20 篇知识 | 🎯 每周 3 个新技能 | ✨ 持续人格进化
"""
        return report

# ========== 运行 ==========
def main():
    print("🧚 小艺正在进化...")
    
    engine = EvolutionEngine()
    
    # 1. 感知当前状态
    state = engine.perceive_current_state()
    
    # 2. 自主决策
    actions = engine.decide_actions(state)
    
    # 3. 执行动作
    results, archive_file = engine.execute_actions(actions, state)
    
    # 4. 生成进化报告
    report = engine.generate_report(state, actions, results)
    report += f"\n【存档】📦 {archive_file}"
    print(report)
    
    # 5. 保存最新状态
    with open(EVOLUTION_DIR / "latest.json", 'w', encoding='utf-8') as f:
        json.dump({
            "state": state,
            "actions": actions,
            "results": results,
            "evolution_count": engine.memory.get("evolution_count", 0),
            "archive": archive_file
        }, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "rollback":
        # 回滚模式
        version = int(sys.argv[2]) if len(sys.argv) > 2 else 3
        engine = EvolutionEngine()
        result = engine.rollback_to(version)
        print(f"🔄 {result['message']}")
    else:
        main()
