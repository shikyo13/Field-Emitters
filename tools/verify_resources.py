"""Validate committed resource JSON without requiring a loader-specific data generator."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
count = 0
for base in [root / "src/main/resources", root / "src/generated/resources"]:
    for path in base.rglob("*.json"):
        data = json.loads(path.read_text())
        if "/recipes/" in str(path) and path.parent.name == "recipes":
            assert "result" in data and isinstance(data["result"], dict), path
            assert data["result"].get("item", "").startswith("fieldemitters:"), path
        count += 1
assert count >= 20
print(f"Validated {count} resource JSON files")
