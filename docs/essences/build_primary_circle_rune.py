"""Draw the Primary Circle Rune at its native 32x32 pixel-art resolution."""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/merlins_inferno/textures/item/primary_circle_rune.png"
PREVIEW = ROOT / "docs/essences/primary_circle_rune_preview.png"

image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)

# Chipped octagonal obsidian seal, with a raised bronze rim.
draw.polygon([(11, 0), (20, 0), (27, 4), (31, 11), (31, 20), (27, 27),
              (20, 31), (11, 31), (4, 27), (0, 20), (0, 11), (4, 4)], fill="#0b0b13")
draw.polygon([(11, 2), (20, 2), (26, 5), (29, 11), (29, 20), (26, 26),
              (20, 29), (11, 29), (5, 26), (2, 20), (2, 11), (5, 5)], fill="#423046")
draw.line([(11, 2), (20, 2), (26, 5), (29, 11)], fill="#b47b51", width=2)
draw.line([(2, 20), (5, 26), (11, 29), (20, 29), (26, 26)], fill="#231b2d", width=2)
draw.line([(3, 11), (6, 5), (11, 2)], fill="#836255", width=1)
draw.line([(29, 20), (26, 26), (20, 29)], fill="#805045", width=1)

# Broken concentric circles make the engraving read as a rune rather than a coin.
draw.ellipse((5, 5, 26, 26), fill="#211a2c", outline="#a86b46", width=1)
draw.arc((5, 5, 26, 26), 204, 327, fill="#ecc48a", width=2)
draw.arc((5, 5, 26, 26), 18, 151, fill="#754044", width=2)
draw.ellipse((8, 8, 23, 23), fill="#2c2035", outline="#6e4b43", width=1)
draw.arc((8, 8, 23, 23), 195, 300, fill="#d2a369", width=1)
draw.arc((8, 8, 23, 23), 15, 125, fill="#b68354", width=1)

# Three inlaid stones recall the fae, infernal and angelic routes.
for x, y, edge, core, shine in [
    (15, 5, "#304e55", "#72c9d4", "#b9f3ef"),
    (6, 19, "#31533c", "#78b975", "#b8e5a2"),
    (23, 19, "#68343a", "#d36b55", "#f4aa76"),
]:
    draw.rectangle((x - 1, y - 1, x + 2, y + 2), fill=edge)
    draw.rectangle((x, y, x + 1, y + 1), fill=core)
    draw.point((x, y), fill=shine)

# Primary-circle sigil: a carved, beveled Roman I, broad enough at item scale.
draw.rectangle((12, 11, 19, 12), fill="#77503a")
draw.rectangle((12, 12, 18, 13), fill="#e2ae6d")
draw.rectangle((14, 13, 17, 20), fill="#9d603e")
draw.rectangle((14, 13, 16, 19), fill="#f1cc87")
draw.rectangle((12, 20, 19, 21), fill="#a06b44")
draw.rectangle((12, 21, 18, 22), fill="#e5b574")
draw.line((14, 13, 14, 19), fill="#fff0b4")
draw.point((17, 16), fill="#e16b5a")

# Incised edge marks and a few glints survive Minecraft's item scaling.
for x, y in [(9, 4), (22, 4), (3, 10), (28, 10), (3, 21), (28, 21), (10, 28), (21, 28)]:
    draw.point((x, y), fill="#c09469")
draw.line((8, 8, 10, 6), fill="#e5bc80")
draw.point((24, 8), fill="#f3d9a2")
draw.point((9, 24), fill="#b98768")

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
image.save(OUTPUT)
image.resize((320, 320), Image.Resampling.NEAREST).save(PREVIEW)
