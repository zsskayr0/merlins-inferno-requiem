"""Sample the user-provided Cascade Vale flowers into transparent 16x16 sprites.

Only background removal and nearest-neighbour resizing are applied. Every
visible output color comes from the supplied image.
"""

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/merlins_inferno/textures/block"


def flower_pixel(rgb):
    r, g, b = rgb
    return (r > g * 1.13 and r > 115 and g < 215) or (g > r * 1.15 and 55 < g < 190)


def extract(source_name, output_name):
    source = Image.open(ROOT / "docs/elysium" / source_name).convert("RGB")
    selected = [(x, y) for y in range(source.height) for x in range(source.width)
                if flower_pixel(source.getpixel((x, y)))]
    left = min(x for x, _ in selected)
    top = min(y for _, y in selected)
    right = max(x for x, _ in selected) + 1
    bottom = max(y for _, y in selected) + 1
    crop = source.crop((left, top, right, bottom))
    width, height = crop.size
    out_width = min(16, round(16 * width / height)) if width <= height else 16
    out_height = min(16, round(16 * height / width)) if height < width else 16
    small = crop.resize((out_width, out_height), Image.Resampling.NEAREST)
    sprite = Image.new("RGBA", (16, 16))
    offset_x = (16 - out_width) // 2
    offset_y = 16 - out_height
    for y in range(out_height):
        for x in range(out_width):
            color = small.getpixel((x, y))
            if flower_pixel(color):
                sprite.putpixel((offset_x + x, offset_y + y), (*color, 255))
    sprite.save(ASSETS / output_name)


def extract_sun_flower():
    """Read the centers of the reference's original 16x16 pixel grid."""
    source = Image.open(ROOT / "docs/elysium/sun_flower_reference.png").convert("RGB")
    sprite = Image.new("RGBA", (16, 16))
    # The artwork spans x=73..662 and y=54..643; the palette is below it.
    for y in range(16):
        for x in range(16):
            rgb = source.getpixel((round(73 + (x + 0.5) * 36.8),
                                   round(54 + (y + 0.5) * 36.8)))
            if not all(channel > 245 for channel in rgb):
                sprite.putpixel((x, y), (*rgb, 255))
    sprite.save(ASSETS / "sun_flower.png")


extract("cascade_blossom_reference.png", "cascade_blossom.png")
extract_sun_flower()

preview = Image.new("RGB", (432, 240), "#eee8ed")
for x, name in ((16, "cascade_blossom"), (224, "sun_flower")):
    sprite = Image.open(ASSETS / f"{name}.png").convert("RGBA")
    enlarged = sprite.resize((192, 192), Image.Resampling.NEAREST)
    preview.paste(enlarged, (x, 16), enlarged)
preview.save(ROOT / "docs/elysium/cascade_flowers_preview.png")
