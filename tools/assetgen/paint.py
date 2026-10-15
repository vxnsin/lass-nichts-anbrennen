"""Color helpers and tile painters for procedurally drawn pixel-art textures."""

from __future__ import annotations

import random

from PIL import Image

RGB = tuple[int, int, int]
RGBA = tuple[int, int, int, int]

TRANSPARENT: RGBA = (0, 0, 0, 0)


def rgb(value: str) -> RGB:
    value = value.lstrip("#")
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16)


# Shared palette
RED = rgb("#c8102e")
RED_DARK = rgb("#8e0b1f")
RED_RIM = rgb("#600816")
WHITE = rgb("#f2f2f2")
OFF_WHITE = rgb("#dcdcd6")
INK = rgb("#50505a")
METAL_LIGHT = rgb("#d6dade")
METAL = rgb("#a5aaaf")
METAL_DARK = rgb("#70757a")
METAL_DARKER = rgb("#484c50")
BLACK = rgb("#1e1e20")
PLASTIC = rgb("#2a2a2e")
PLASTIC_LIGHT = rgb("#4a4a50")
GREY = rgb("#8a8d91")
LIGHT_GREY = rgb("#c5cac6")  # RAL 7035, typical fire door color
SAFETY_GREEN = rgb("#1f8a4c")
SAFETY_YELLOW = rgb("#f2c200")
BLUE = rgb("#2b6cd8")
ORANGE = rgb("#f28c00")
HI_VIS = rgb("#dff21f")
WOOD = rgb("#a8743f")
WOOD_DARK = rgb("#7a522b")
COPPER = rgb("#c87533")
BRASS = rgb("#c9a43a")
GLASS = rgb("#a8d4e6")
OIL = rgb("#e0b030")

# Brightness across a face that imitates a curved surface lit from the front left.
CYLINDER_PROFILE = (0.78, 0.9, 1.08, 1.15, 1.06, 1.0, 1.0, 0.97, 0.93, 0.88, 0.82, 0.74)


def shade(color: RGB, factor: float = 1.0, alpha: int = 255) -> RGBA:
    return (*(max(0, min(255, round(channel * factor))) for channel in color), alpha)


def column_profile(width: int, kind: str | None) -> list[float]:
    """Per-column brightness factors: 'cylinder' (curved), 'bevel' (darker edges) or flat."""
    if kind == "cylinder":
        last = len(CYLINDER_PROFILE) - 1
        return [CYLINDER_PROFILE[round(x * last / max(1, width - 1))] for x in range(width)]
    if kind == "bevel":
        return [0.86 if x in (0, width - 1) else 1.0 for x in range(width)]
    return [1.0] * width


def canvas(width: int, height: int, fill: RGBA = TRANSPARENT) -> Image.Image:
    return Image.new("RGBA", (width, height), fill)


def tile(color: RGB, width: int = 8, height: int = 8, *, profile: str | None = None, vertical: float = 0.0,
         noise: float = 0.0, border: RGB | None = None, seed: int = 0) -> Image.Image:
    """Uniform material tile with optional curvature shading, vertical gradient, noise and a 1 px border.

    vertical > 0 brightens the top and darkens the bottom by that fraction.
    """
    rng = random.Random(seed)
    columns = column_profile(width, profile)
    image = canvas(width, height)
    for y in range(height):
        gradient = 1 + vertical * (0.5 - y / max(1, height - 1))
        for x in range(width):
            base = border if border is not None and (x in (0, width - 1) or y in (0, height - 1)) else color
            factor = columns[x] * gradient * (1 + rng.uniform(-noise, noise))
            image.putpixel((x, y), shade(base, factor))
    return image


def grid(rows: list[str], palette: dict[str, RGB | RGBA]) -> Image.Image:
    """Pixel grid from strings; '.' is transparent, every other symbol must be in the palette."""
    width = len(rows[0])
    if any(len(row) != width for row in rows):
        raise ValueError("all grid rows must have the same width")
    image = canvas(width, len(rows))
    for y, row in enumerate(rows):
        for x, symbol in enumerate(row):
            if symbol == ".":
                continue
            color = palette[symbol]
            image.putpixel((x, y), color if len(color) == 4 else shade(color))
    return image


def apply_profile(image: Image.Image, profile: str) -> Image.Image:
    """Applies column shading to an existing tile (e.g. a label painted onto a curved body)."""
    columns = column_profile(image.width, profile)
    result = image.copy()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a:
                result.putpixel((x, y), shade((r, g, b), columns[x], a))
    return result


def stripes(first: RGB, second: RGB, width: int, height: int, period: int = 4) -> Image.Image:
    """Diagonal warning stripes."""
    image = canvas(width, height)
    for y in range(height):
        for x in range(width):
            image.putpixel((x, y), shade(first if ((x + y) // (period // 2)) % 2 == 0 else second))
    return image


def perforated(color: RGB, hole: RGB, width: int, height: int, step: int = 2) -> Image.Image:
    """Sheet metal with a regular hole pattern (ventilation grilles, cabinet doors)."""
    image = tile(color, width, height, border=tuple(int(c * 0.7) for c in color))
    for y in range(1, height - 1, step):
        for x in range(1 + (y // step) % 2, width - 1, step):
            image.putpixel((x, y), shade(hole))
    return image


def rect(image: Image.Image, x0: int, y0: int, x1: int, y1: int, color: RGB) -> Image.Image:
    """Fills the inclusive rectangle (x0, y0)-(x1, y1)."""
    for y in range(max(0, y0), min(image.height, y1 + 1)):
        for x in range(max(0, x0), min(image.width, x1 + 1)):
            image.putpixel((x, y), shade(color))
    return image


def outline(image: Image.Image, x0: int, y0: int, x1: int, y1: int, color: RGB) -> Image.Image:
    """Draws a 1 px rectangle outline (inclusive coordinates)."""
    rect(image, x0, y0, x1, y0, color)
    rect(image, x0, y1, x1, y1, color)
    rect(image, x0, y0, x0, y1, color)
    rect(image, x1, y0, x1, y1, color)
    return image


def circle(image: Image.Image, cx: float, cy: float, radius: float, color: RGB, *, ring: float = 0.0) -> Image.Image:
    """Filled circle, or a ring of the given thickness when ring > 0."""
    for y in range(image.height):
        for x in range(image.width):
            distance = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if distance <= radius and (ring <= 0 or distance > radius - ring):
                image.putpixel((x, y), shade(color))
    return image


def overlay(base: Image.Image, top: Image.Image, x: int, y: int) -> Image.Image:
    base.alpha_composite(top, (x, y))
    return base


def label(width: int, height: int, background: RGB, lines: list[tuple[int, RGB]], *,
          text_rows: tuple[int, ...] = (), ink: RGB = (80, 80, 90)) -> Image.Image:
    """Label tile: background, full-width colored lines at given rows and dashed 'small print' rows."""
    image = tile(background, width, height)
    for row, color in lines:
        for x in range(width):
            image.putpixel((x, row), shade(color))
    for row in text_rows:
        for x in range(2, width - 2):
            if x % 3 != 1:
                image.putpixel((x, row), shade(ink))
    return image
