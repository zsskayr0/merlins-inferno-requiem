"""Overlay moss pixels on the existing 16x16 Arganite texture."""

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEXTURES = ROOT / "src/main/resources/assets/merlins_inferno/textures/block"
stone = Image.open(TEXTURES / "arganite.png").convert("RGBA")
moss = stone.copy()

mask = (
    "................",
    "..mm........mm..",
    ".mmmm.......mmm.",
    ".mmm..m....mmm..",
    "...mmmm...mm....",
    "....mmm..mmm....",
    ".........mm.....",
    ".....mm.........",
    "....mmm....m....",
    "...mmmm...mmm...",
    "....mm...mmmm...",
    "..........mm....",
    ".m....mm........",
    ".mm..mmmm....m..",
    "..mm..mm....mm..",
    "................",
)
palette = {
    (145, 149, 155): (62, 88, 58, 255),
    (156, 160, 166): (74, 102, 64, 255),
    (169, 173, 178): (88, 118, 74, 255),
    (185, 189, 194): (105, 132, 82, 255),
    (207, 209, 213): (126, 149, 98, 255),
}
assert len(mask) == 16 and all(len(row) == 16 for row in mask)
for y, row in enumerate(mask):
    for x, mark in enumerate(row):
        if mark == "m":
            moss.putpixel((x, y), palette[stone.getpixel((x, y))[:3]])

moss.save(TEXTURES / "mossy_arganite.png")
