#!/usr/bin/env python3
"""Export the square showcase logo for each platform from the rendered master."""

import hashlib
import io
import json
from pathlib import Path

from PIL import Image


root = Path(__file__).resolve().parents[3]
output = root / "docs/publishing"
master_file = output / "field-emitters-logo-master.png"
master = Image.open(master_file).convert("RGB")
if master.size != (2048, 2048):
    raise SystemExit("Render the 2048-square master before exporting icons.")

curseforge = master.resize((400, 400), Image.Resampling.LANCZOS)
curseforge.save(output / "curseforge-icon.png", optimize=True, compress_level=9)

# Keep a lossless original below Modrinth's 256 KiB upload ceiling. Its service
# separately prepares a small thumbnail and retains the original upload.
limit = 256 * 1024
for size in (2048, 1792, 1536, 1280, 1024, 896, 768, 640, 512):
    candidate = master if size == 2048 else master.resize((size, size), Image.Resampling.LANCZOS)
    encoded = io.BytesIO()
    candidate.save(encoded, format="WEBP", lossless=True, quality=100, method=6, exact=True)
    data = encoded.getvalue()
    print(f"Lossless {size} x {size}: {len(data)} bytes", flush=True)
    if len(data) < limit:
        (output / "modrinth-icon.webp").write_bytes(data)
        if Image.open(io.BytesIO(data)).convert("RGB").tobytes() != candidate.tobytes():
            raise SystemExit("The WebP export changed a source pixel.")
        break
else:
    raise SystemExit("No lossless export fits Modrinth's icon limit.")

records = []
for name in (master_file.name, "curseforge-icon.png", "modrinth-icon.webp"):
    path = output / name
    data = path.read_bytes()
    with Image.open(path) as image:
        records.append({"file": name, "width": image.width, "height": image.height,
                        "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})

(output / "logo-manifest.json").write_text(json.dumps({
    "composition": "ProjectIcon",
    "reference": "docs/media/showcase-poster.png",
    "source": "tools/showcase/src/ProjectIcon.tsx",
    "render_scale": 2,
    "model_pixel_ratio": 2,
    "exports": records,
    "curseforge": {"format": "PNG", "dimensions": [400, 400],
                   "requirements": "https://support.curseforge.com/support/solutions/articles/9000199552"},
    "modrinth": {"maximum_bytes": limit, "format": "lossless WebP",
                 "requirements": "https://docs.modrinth.com/api/operations/changeprojecticon/",
                 "original_retained": True},
}, indent=2) + "\n")
print("Exported platform icons and logo-manifest.json.")
