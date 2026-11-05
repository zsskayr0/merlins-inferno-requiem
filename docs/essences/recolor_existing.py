"""Rebuild the original four essence icons with a richer 16x16 palette."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent / "source"
TEXTURES = ROOT / "src/main/resources/assets/merlins_inferno/textures/item"

PALETTES = {
    "infernal": {
        (3, 3, 5): (8, 4, 10),
        (21, 2, 0): (39, 8, 15),
        (103, 0, 22): (122, 22, 27),
        (173, 1, 15): (190, 42, 18),
        (224, 53, 0): (239, 75, 15),
        (252, 116, 6): (255, 139, 25),
        (253, 115, 8): (255, 151, 38),
        (252, 197, 53): (255, 226, 118),
        (10, 6, 31): (23, 10, 22),
        (7, 23, 39): (24, 13, 25),
        (11, 38, 59): (51, 22, 31),
        (27, 48, 77): (74, 31, 42),
    },
    "angelic": {
        (3, 3, 5): (5, 12, 19),
        (0, 14, 15): (9, 29, 42),
        (46, 69, 69): (47, 99, 120),
        (80, 119, 121): (74, 151, 173),
        (142, 190, 218): (137, 208, 224),
        (229, 254, 255): (221, 250, 255),
        (231, 254, 255): (243, 255, 250),
        (255, 255, 255): (255, 247, 210),
        (10, 6, 31): (9, 24, 42),
        (7, 23, 39): (11, 37, 56),
        (11, 38, 59): (22, 60, 81),
        (27, 48, 77): (39, 88, 111),
    },
    "fae": {
        (4, 3, 3): (10, 6, 19),
        (22, 5, 4): (29, 12, 36),
        (15, 9, 17): (23, 13, 32),
        (34, 16, 26): (51, 27, 70),
        (53, 27, 43): (76, 37, 95),
        (68, 38, 52): (105, 53, 126),
        (70, 61, 74): (90, 52, 120),
        (123, 106, 128): (156, 93, 177),
        (211, 171, 200): (234, 153, 215),
        (255, 245, 255): (253, 225, 252),
        (255, 246, 255): (255, 213, 239),
        (255, 255, 255): (204, 255, 240),
    },
    "mundane": {
        (3, 3, 5): (15, 13, 11),
        (21, 2, 0): (33, 32, 23),
        (22, 44, 1): (51, 64, 36),
        (37, 76, 2): (76, 94, 48),
        (22, 133, 31): (110, 135, 64),
        (26, 181, 106): (154, 176, 91),
        (26, 180, 109): (172, 187, 99),
        (126, 209, 193): (246, 215, 137),
        (10, 6, 31): (32, 28, 23),
        (7, 23, 39): (43, 39, 29),
        (11, 38, 59): (66, 57, 36),
        (27, 48, 77): (91, 76, 46),
    },
}

preview = Image.new("RGBA", (16 * 4 * 12, 16 * 2 * 12), (28, 28, 32, 255))
for column, (name, palette) in enumerate(PALETTES.items()):
    source = Image.open(SOURCE / f"essence_{name}.png").convert("RGBA")
    assert source.size == (16, 16)
    recolored = source.copy()
    recolored.putdata([(*palette.get(pixel[:3], pixel[:3]), pixel[3]) for pixel in source.getdata()])
    assert [pixel[3] for pixel in recolored.getdata()] == [pixel[3] for pixel in source.getdata()]
    recolored.save(TEXTURES / f"essence_{name}.png", optimize=True)
    preview.alpha_composite(source.resize((192, 192), Image.Resampling.NEAREST), (column * 192, 0))
    preview.alpha_composite(recolored.resize((192, 192), Image.Resampling.NEAREST), (column * 192, 192))

preview.save(Path(__file__).resolve().parent / "recolor_preview.png")
