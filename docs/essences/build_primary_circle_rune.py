"""Draw the 16x16 Primary Circle Rune used by the Pandora ritual."""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/merlins_inferno/textures/item/primary_circle_rune.png"

image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)
draw.ellipse((0, 0, 15, 15), fill="#14101c", outline="#0b0912")
draw.ellipse((1, 1, 14, 14), fill="#47304a", outline="#8c674f")
draw.arc((1, 1, 14, 14), 195, 340, fill="#e7bf7c", width=1)
draw.arc((1, 1, 14, 14), 20, 160, fill="#5b3d43", width=1)
draw.ellipse((3, 3, 12, 12), fill="#211b2d", outline="#b78650")
draw.point((4, 4), fill="#efd49b")
draw.point((11, 4), fill="#efd49b")
draw.point((4, 11), fill="#8e4b4d")
draw.point((11, 11), fill="#8e4b4d")
draw.line((5, 6, 10, 6), fill="#dcb57b")
draw.line((5, 9, 10, 9), fill="#9f693f")
draw.line((7, 5, 7, 10), fill="#f1dbab")
draw.line((8, 5, 8, 10), fill="#c99558")
draw.point((7, 7), fill="#ffedbd")
draw.point((8, 8), fill="#b53d49")
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
image.save(OUTPUT)
