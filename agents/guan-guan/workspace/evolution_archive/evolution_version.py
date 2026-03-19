import sys
import os

# 解决 Windows 控制台编码问题
if sys.platform == "win32":
    os.environ["PYTHONIOENCODING"] = "utf-8"
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except:
        pass

import json
import shutil
from datetime import datetime
from pathlib import Path

WORKSPACE = Path(r"C:\Users\16343\.openclaw\agents\guan-guan\workspace")
ARCHIVE_DIR = WORKSPACE / "evolution_archive"
INDEX_FILE = ARCHIVE_DIR / "index.md"

# 需要存档的关键文件
KEY_FILES = [
    "evolution-plan.md",
    "knowledge/",
    "self-evolution_system.md",
    "MEMORY.md",
]


def get_current_round() -> int:
    """获取当前进化次数"""
    if not INDEX_FILE.exists():
        return 0
    
    content = INDEX_FILE.read_text(encoding="utf-8")
    for line in content.split("\n"):
        if "当前进化次数" in line:
            try:
                return int(line.split(":")[-1].strip())
            except:
                pass
    return 0


def save_evolution(notes: str, templates: str, cases: str, score: int, improvements: list):
    """保存一次进化记录"""
    round_num = get_current_round() + 1
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    # 创建本次进化记录
    record = {
        "round": round_num,
        "timestamp": timestamp,
        "notes": notes,
        "templates": templates,
        "cases": cases,
        "score": score,
        "improvements": improvements,
    }
    
    # 保存 JSON 格式（便于程序读取）
    json_file = ARCHIVE_DIR / f"round_{round_num:03d}.json"
    json_file.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
    
    # 保存 Markdown 格式（便于人类阅读）
    md_content = f"""# 进化 #{round_num}

> 时间: {timestamp}
> 价值分: {score}

## 产出

- 笔记: {notes}
- 模板: {templates}
- 案例: {cases}

## 优化改进

{chr(10).join(f"- {imp}" for imp in improvements)}

---

*自动记录于 {timestamp}*
"""
    md_file = ARCHIVE_DIR / f"round_{round_num:03d}.md"
    md_file.write_text(md_content, encoding="utf-8")
    
    # 更新索引
    update_index(round_num, timestamp, score)
    
    print(f"✅ 进化 #{round_num} 已存档 (价值分: {score})")
    return round_num


def update_index(round_num: int, timestamp: str, score: int):
    """更新索引文件"""
    if not INDEX_FILE.exists():
        content = """# 进化记录索引

> 管管进化版本控制系统

---

## 当前状态

- **当前进化次数**: 0
- **当前版本**: V1.0

---

## 进化历史

| 次数 | 日期 | 版本 | 价值分 | 状态 |
|------|------|------|--------|------|
"""
    else:
        content = INDEX_FILE.read_text(encoding="utf-8")
    
    # 追加新记录
    version = f"V{round_num // 5 + 1}.{round_num % 5}"
    new_row = f"| {round_num} | {timestamp} | {version} | {score} | ✅ |\n"
    
    # 找到表格末尾并插入
    lines = content.split("\n")
    for i, line in enumerate(lines):
        if line.startswith("| ---"):
            lines[i] = new_row + line
            break
    
    # 更新当前次数
    for i, line in enumerate(lines):
        if "当前进化次数" in line:
            lines[i] = f"- **当前进化次数**: {round_num}"
            break
    
    INDEX_FILE.write_text("\n".join(lines), encoding="utf-8")


def rollback(round_num: int):
    """回滚到指定进化"""
    json_file = ARCHIVE_DIR / f"round_{round_num:03d}.json"
    
    if not json_file.exists():
        print(f"❌ 进化 #{round_num} 不存在")
        return False
    
    print(f"⚠️ 确认回滚到进化 #{round_num}?")
    print(f"   输入 'yes' 确认: ", end="")
    
    # 注意：在自动化脚本中需要特殊处理
    # 这里只打印信息，实际回滚需要手动确认
    
    print(f"""
🔄 回滚步骤:
1. 读取 round_{round_num:03d}.json
2. 恢复当时的关键文件
3. 更新当前版本指针
""")
    return True


def show_history():
    """显示进化历史"""
    current = get_current_round()
    print(f"\n📜 进化历史 (共 {current} 次)\n")
    print("| 次数 | 时间 | 价值分 |")
    print("|------|------|--------|")
    
    for i in range(1, current + 1):
        json_file = ARCHIVE_DIR / f"round_{i:03d}.json"
        if json_file.exists():
            data = json.loads(json_file.read_text(encoding="utf-8"))
            print(f"| {i} | {data['timestamp']} | {data['score']} |")
    
    print()


if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1:
        if sys.argv[1] == "history":
            show_history()
        elif sys.argv[1] == "save" and len(sys.argv) > 5:
            # save notes templates cases score improvements
            notes = sys.argv[2]
            templates = sys.argv[3]
            cases = sys.argv[4]
            score = int(sys.argv[5])
            improvements = sys.argv[6].split(",") if len(sys.argv) > 6 else []
            save_evolution(notes, templates, cases, score, improvements)
        elif sys.argv[1] == "rollback" and len(sys.argv) > 2:
            rollback(int(sys.argv[2]))
    else:
        print("管管进化版本控制系统")
        print("  history              - 查看进化历史")
        print("  save n t c s \"i1,i2\" - 保存进化记录")
        print("  rollback N           - 回滚到第N次进化")
