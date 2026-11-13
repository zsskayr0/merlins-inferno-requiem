"""Render the WallGeode Java face profile as a block-by-block front schematic."""

from pathlib import Path
import re
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[2]
source = (root / "src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/structure/WallGeode.java").read_text()


def array(name):
    match = re.search(rf"\b{name}\s*=\s*\{{([^}}]+)\}}", source)
    assert match, name
    values = [int(value) for value in re.findall(r"\d+", match.group(1))]
    assert len(values) == 27
    return values


left, right, band = array("LEFT"), array("RIGHT"), array("BAND")
image = Image.new("RGB", (736, 810), "#332c32")
draw = ImageDraw.Draw(image)


def cell(col, row, color):
    x, y = 17 + col * 24, row * 24
    draw.rectangle((x, y, x + 23, y + 23), fill=color, outline="#211d25", width=1)


for row in range(30):
    for col in range(30):
        cell(col, row, "#676262" if (col * 17 + row * 29) % 7 else "#575558")

for row in range(2, 29):
    for col in range(left[row - 2], right[row - 2] + 1):
        edge = min(col - left[row - 2], right[row - 2] - col)
        color = "#d9d1bd" if edge == 0 else ("#8b4e9b" if edge <= band[row - 2] else "#261b31")
        cell(col, row, color)

# The two-level refuge sits behind the opening. The exact blocks are chosen
# deterministically in Java; this front view shows its architectural footprint.
for row in range(12, 25):
    for col in range(8, 21):
        if row in (12, 24) or col in (8, 20) or (col * 13 + row * 7) % 11:
            cell(col, row, "#777373" if (col + row) % 5 else "#615f62")

for col in (10, 13, 17):
    for row in (14, 15):
        cell(col, row, "#333640")
for col in (10, 16):
    for row in (19, 20):
        cell(col, row, "#333640")
for col in (13, 14):
    for row in (22, 23):
        cell(col, row, "#372824")
for col in (18, 19):
    for row in (16, 17):
        cell(col, row, "#352730")

for row, col in ((5, 9), (5, 17), (7, 6), (8, 22), (10, 5), (11, 23),
                 (13, 4), (14, 23), (16, 5), (18, 22), (21, 6), (23, 20),
                 (25, 10), (25, 18)):
    x, y = 17 + col * 24 + 12, row * 24 + 12
    draw.polygon(((x, y - 10), (x + 8, y), (x, y + 10), (x - 8, y)), fill="#cb9bd2")

draw.text((22, 753), "CALCITE  /  AMETHYST  /  DARK CAVITY  /  STONE REFUGE", fill="#f3e9f0")
draw.text((22, 777), "Each grid square = one block; front view from the island wall.", fill="#d4c8d1")
image.save(root / "docs/elysium/wall_geode_blueprint.png")
