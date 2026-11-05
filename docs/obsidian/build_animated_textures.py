"""Build the three 16x16 animated obsidian texture strips from source tiles."""

from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image


SOURCE_DIR = Path(__file__).resolve().parent
TEXTURE_DIR = SOURCE_DIR.parents[1] / "src/main/resources/assets/merlins_inferno/textures/block"


def clamp(value: float) -> int:
    return max(0, min(255, round(value)))


def purified_source() -> Image.Image:
    # The concept is a larger pixel-art image; average each cell into one game pixel.
    image = Image.open(SOURCE_DIR / "purified_concept.png").convert("RGB")
    return image.resize((16, 16), Image.Resampling.BOX)


def mossy_source() -> Image.Image:
    image = Image.open(SOURCE_DIR / "mossy_concept.png").convert("RGB")
    return image.resize((16, 16), Image.Resampling.BOX)


def frame(base: Image.Image, kind: str, index: int, count: int) -> Image.Image:
    result = base.copy().convert("RGB")
    pixels = result.load()
    for y in range(16):
        for x in range(16):
            r, g, b = pixels[x, y]
            phase = 2 * math.pi * index / count
            if kind == "corrupted":
                # Red fissures crawl unevenly down the stone; purple rock stays dark.
                if r > 58 and r > g * 2 and r > b * 1.2:
                    wave = (1 + math.sin(phase - y * 0.65 + x * 0.38)) / 2
                    spark = wave > 0.88 and (x * 7 + y * 3) % 5 == 0
                    pixels[x, y] = (
                        clamp(r * (0.79 + 0.40 * wave) + 25 * spark),
                        clamp(g + 15 * wave + 12 * spark),
                        clamp(b + 5 * wave),
                    )
            elif kind == "withered":
                # A weak, slow violet pulse appears only on the raised cracks.
                if r >= 24 and b >= 30:
                    wave = (1 + math.sin(phase + x * 0.38 + y * 0.27)) / 2
                    pixels[x, y] = (
                        clamp(r + 7 * wave),
                        clamp(g + 4 * wave),
                        clamp(b + 30 * wave),
                    )
            elif kind == "purified":
                # Light flows through the pale cyan and gold veins.
                if max(r, g, b) >= 48 and (b > r * 1.12 or r > b * 1.15):
                    wave = (1 + math.sin(phase + y * 0.57 - x * 0.42)) / 2
                    glow = 0.75 + 0.65 * wave
                    if b > r * 1.12:
                        pixels[x, y] = (clamp(r * glow), clamp(g * glow + 9 * wave), clamp(b * glow + 16 * wave))
                    else:
                        pixels[x, y] = (clamp(r * glow + 17 * wave), clamp(g * glow + 11 * wave), clamp(b * glow))
            else:
                # Moss breathes gently while the obsidian underneath remains still.
                if g > r * 1.12 and g > b * 1.2 and g >= 34:
                    wave = (1 + math.sin(phase + y * 0.42 + x * 0.2)) / 2
                    pixels[x, y] = (clamp(r + 5 * wave), clamp(g * (0.82 + 0.38 * wave)), clamp(b + 4 * wave))
    return result


def build(name: str, source: Image.Image, count: int, frametime: int) -> None:
    source = source.convert("RGB")
    strip = Image.new("RGB", (16, 16 * count))
    for index in range(count):
        strip.paste(frame(source, name, index, count), (0, index * 16))
    target = TEXTURE_DIR / f"{name}_obsidian.png"
    strip.save(target, optimize=True)
    target.with_suffix(".png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": frametime}}, indent=2) + "\n",
        encoding="utf-8",
    )


build("corrupted", Image.open(SOURCE_DIR / "corrupted_source.png"), 8, 3)
build("withered", Image.open(SOURCE_DIR / "withered_source.png"), 10, 5)
build("purified", purified_source(), 8, 4)
build("mossy", mossy_source(), 8, 6)
