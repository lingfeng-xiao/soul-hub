import json

with open(r'C:\Users\16343\.openclaw\agents\xiao-yi\workspace\perception_data\memory.json', 'r', encoding='utf-8') as f:
    d = json.load(f)

perceptions = d.get('perceptions', [])
print(f"总感知记录: {len(perceptions)}")

# 取最近20条
recent = perceptions[-20:]
for p in recent:
    ts = p.get('timestamp', '')[:19]
    mem = p.get('hardware', {}).get('memory', {}).get('used_pct', 0)
    print(f"{ts} -> {mem}%")
