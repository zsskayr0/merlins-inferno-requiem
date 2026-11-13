"""Add small flowers to variants of the existing Cascade Grass top texture.

The petal colors were sampled from cascade_flower_reference.png. The pink
background comes unchanged from cascade_grass_top.png.
"""

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEXTURES = ROOT / "src/main/resources/assets/merlins_inferno/textures/block"
base = Image.open(TEXTURES / "cascade_grass_top.png").convert("RGBA")
petal = (239, 248, 209, 255)
center = (255, 255, 220, 255)

for index, (cx, cy) in enumerate(((5, 7), (11, 4), (3, 12), (12, 11)), start=1):
    tile = base.copy()
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        tile.putpixel((cx + dx, cy + dy), petal)
    tile.putpixel((cx, cy), center)
    tile.save(TEXTURES / f"cascade_grass_top_flowery_{index}.png")
