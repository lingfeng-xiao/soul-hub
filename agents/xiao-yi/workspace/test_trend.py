import json
import sys
sys.path.insert(0, r'C:\Users\16343\.openclaw\agents\xiao-yi\workspace')

from pathlib import Path

WORKSPACE = Path(r'C:\Users\16343\.openclaw\agents\xiao-yi\workspace')
PERCEPTION_DIR = WORKSPACE / 'perception_data'

def load_memory():
    memory_file = PERCEPTION_DIR / "memory.json"
    if memory_file.exists():
        with open(memory_file, 'r', encoding='utf-8') as f:
            return json.load(f)
    return {"perceptions": []}

def get_memory_trend():
    """获取内存趋势"""
    memory = load_memory()
    recent = memory.get("perceptions", [])[-10:] if memory.get("perceptions") else []
    
    print(f"最近感知记录数: {len(recent)}")
    
    if len(recent) < 3:
        return "unknown", 0
    
    mem_values = []
    for p in recent:
        hw = p.get("hardware", {})
        print(f"硬件数据: {hw}")
        mem = hw.get("memory", {}).get("used_pct", 0)
        print(f"内存: {mem}")
        if mem > 0:
            mem_values.append(mem)
    
    print(f"有效内存值: {mem_values}")
    
    if len(mem_values) < 3:
        return "unknown", 0
    
    mid = len(mem_values) // 2
    first_half_avg = sum(mem_values[:mid]) / mid
    second_half_avg = sum(mem_values[mid:]) / (len(mem_values) - mid)
    
    diff = second_half_avg - first_half_avg
    print(f"前半均: {first_half_avg}, 后半均: {second_half_avg}, 差值: {diff}")
    
    if diff > 2:
        return "rising", round(diff, 1)
    elif diff < -2:
        return "falling", round(abs(diff), 1)
    else:
        return "stable", round(diff, 1)

trend, val = get_memory_trend()
print(f"趋势: {trend}, 值: {val}")
