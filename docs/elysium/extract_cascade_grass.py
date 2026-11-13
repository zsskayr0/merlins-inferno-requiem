"""Sample the 16x16 pink grass tiles from the supplied pixel-art reference.

The screenshot shows each source pixel enlarged to roughly 18 screen pixels.
Sampling tile centres preserves the supplied RGB values without interpolation.
Only non-pink side pixels are replaced with the project's Arganite pixels.
"""

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "docs/elysium/cascade_grass_reference.png"
TEXTURES = ROOT / "src/main/resources/assets/merlins_inferno/textures/block"
ARGANITE = Image.open(TEXTURES / "arganite.png").convert("RGBA")
REFERENCE = Image.open(SOURCE).convert("RGB")


def sample_tile(left: int, top: int) -> Image.Image:
    tile = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            sx = left + int((x + 0.5) * 286 / 16)
            sy = top + int((y + 0.5) * 286 / 16)
            tile.putpixel((x, y), (*REFERENCE.getpixel((sx, sy)), 255))
    return tile


top = sample_tile(914, 0)
side_reference = sample_tile(914, 304)
side = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        pink = side_reference.getpixel((x, y))
        is_pink = pink[0] - pink[1] > 20 and pink[0] - pink[2] > 8
        side.putpixel((x, y), pink if is_pink else ARGANITE.getpixel((x, y)))

top.save(TEXTURES / "cascade_grass_top.png")
side.save(TEXTURES / "cascade_grass_side.png")
