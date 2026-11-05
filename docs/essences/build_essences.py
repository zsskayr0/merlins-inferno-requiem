"""Recolor the existing 16x16 essence silhouettes without changing their size."""

from pathlib import Path

from PIL import Image


TEXTURES = Path(__file__).resolve().parents[2] / "src/main/resources/assets/merlins_inferno/textures/item"
SOURCE = Path(__file__).resolve().parent / "source"

DAEDRIC = {
    (21, 2, 0): (20, 0, 22),
    (103, 0, 22): (91, 0, 69),
    (224, 53, 0): (185, 12, 84),
    (253, 115, 8): (242, 37, 119),
    (173, 1, 15): (136, 0, 83),
    (252, 197, 53): (255, 120, 198),
    (252, 116, 6): (247, 49, 131),
    (11, 38, 59): (40, 11, 50),
    (7, 23, 39): (29, 7, 38),
    (27, 48, 77): (57, 16, 72),
}

CELESTIAL = {
    (0, 14, 15): (16, 8, 43),
    (46, 69, 69): (67, 42, 116),
    (142, 190, 218): (145, 116, 232),
    (229, 254, 255): (235, 215, 255),
    (80, 119, 121): (105, 78, 181),
    (255, 255, 255): (255, 242, 179),
    (231, 254, 255): (240, 218, 255),
    (11, 38, 59): (28, 23, 69),
    (7, 23, 39): (20, 16, 54),
    (27, 48, 77): (52, 41, 104),
}


def recolor(source: str, target: str, palette: dict[tuple[int, int, int], tuple[int, int, int]]) -> None:
    image = Image.open(SOURCE / source).convert("RGBA")
    image.putdata([(*palette.get(pixel[:3], pixel[:3]), pixel[3]) for pixel in image.getdata()])
    image.save(TEXTURES / target, optimize=True)


recolor("essence_infernal.png", "essence_daedric.png", DAEDRIC)
recolor("essence_angelic.png", "essence_celestial.png", CELESTIAL)
