"""Recolor vanilla short and tall grass with the same Cascade Vale palette."""

from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar"
OUT = ROOT / "src/main/resources/assets/merlins_inferno/textures/block"

shades = (
    (132, 65, 114),
    (149, 76, 128),
    (171, 95, 145),
    (194, 118, 165),
    (216, 143, 183),
    (239, 177, 205),
)
with ZipFile(ASSETS) as archive:
    for source_name, output_name, palette in (
        ("short_grass.png", "pink_grass.png", shades),
        ("tall_grass_bottom.png", "tall_pink_grass_bottom.png", shades[1:]),
        ("tall_grass_top.png", "tall_pink_grass_top.png", shades[1:]),
    ):
        source = Image.open(BytesIO(archive.read("assets/minecraft/textures/block/" + source_name))).convert("RGBA")
        levels = sorted({pixel[0] for pixel in source.get_flattened_data() if pixel[3]})
        assert len(levels) == len(palette), source_name
        colors = dict(zip(levels, palette))
        result = Image.new("RGBA", source.size)
        for y in range(source.height):
            for x in range(source.width):
                red, _, _, alpha = source.getpixel((x, y))
                if alpha:
                    result.putpixel((x, y), (*colors[red], alpha))
        result.save(OUT / output_name)
