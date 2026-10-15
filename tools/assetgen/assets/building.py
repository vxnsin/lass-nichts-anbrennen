"""Building fire protection: extinguisher bracket, call point, smoke detector, sprinkler, hose cabinet,
fire door, door wedge, battery quarantine box and battery charging cabinet.

Wall-mounted models have their front on the north face and their back at z = 16.
"""

from __future__ import annotations

from .. import paint as p
from ..atlas import Atlas
from ..model import Model
from ..output import Asset


def feuerloescher_halter() -> Asset:
    atlas = Atlas()
    atlas.add("metal", p.tile(p.METAL_DARK, 8, 8, vertical=0.3))
    atlas.add("plate", p.tile(p.METAL_DARKER, 8, 8, profile="bevel"))
    model = Model("wall")
    model.box("plate", (6, 2, 15), (10, 14, 16), "plate")
    model.box("hook", (6.5, 12, 11), (9.5, 13, 15), "metal")
    model.box("hook_lip", (6.5, 13, 11), (9.5, 14, 12), "metal")
    model.box("strap", (5, 4, 11), (11, 5, 15), "metal")
    return Asset("feuerloescher_halter", "furniture", "Feuerlöscher-Wandhalter", atlas, {"": model})


def druckknopfmelder() -> Asset:
    """Manual fire alarm call point (red housing, glass window, push button)."""
    atlas = Atlas()
    front = p.tile(p.RED, 12, 12, border=p.RED_DARK)
    p.rect(front, 2, 2, 9, 9, p.rgb("#e8eef0"))
    p.circle(front, 5.5, 5.5, 2.4, p.BLACK)
    p.circle(front, 5.5, 5.5, 1.2, p.METAL_DARK)
    p.rect(front, 3, 10, 8, 10, p.WHITE)
    atlas.add("front", front)
    atlas.add("housing", p.tile(p.RED, 8, 8, profile="bevel"))
    model = Model("wall")
    model.box("housing", (4, 4, 13), (12, 12, 16), "housing", north="front")
    return Asset("druckknopfmelder", "furniture", "Druckknopfmelder", atlas, {"": model})


def rauchmelder() -> Asset:
    """Ceiling smoke detector (mounted at the top of the block)."""
    atlas = Atlas()
    bottom = p.tile(p.WHITE, 12, 12)
    p.circle(bottom, 5.5, 5.5, 4.5, p.OFF_WHITE, ring=1.0)
    p.circle(bottom, 5.5, 5.5, 2.5, p.OFF_WHITE, ring=1.0)
    bottom.putpixel((8, 3), p.shade(p.rgb("#ff2020")))  # status LED
    atlas.add("bottom", bottom)
    atlas.add("shell", p.tile(p.WHITE, 8, 4, vertical=0.15))
    model = Model("block")
    model.box("base", (4, 15, 4), (12, 16, 12), "shell")
    model.box("dome", (4.5, 13.5, 4.5), (11.5, 15, 11.5), "shell", down="bottom")
    return Asset("rauchmelder", "furniture", "Rauchmelder", atlas, {"": model})


def sprinkler() -> Asset:
    atlas = Atlas()
    atlas.add("brass", p.tile(p.BRASS, 8, 8, vertical=0.3))
    atlas.add("bulb", p.tile(p.rgb("#e02a2a"), 4, 4, vertical=0.4))
    model = Model("block")
    model.box("pipe", (7.25, 13, 7.25), (8.75, 16, 8.75), "brass")
    model.box("arm_west", (6.5, 10, 7.75), (7, 13, 8.25), "brass")
    model.box("arm_east", (9, 10, 7.75), (9.5, 13, 8.25), "brass")
    model.box("deflector", (5.5, 9.5, 5.5), (10.5, 10, 10.5), "brass")
    model.box("bulb", (7.6, 10.25, 7.6), (8.4, 12.75, 8.4), "bulb")
    return Asset("sprinkler", "furniture", "Sprinklerkopf", atlas, {"": model})


def wandhydrant() -> Asset:
    """Wall hose cabinet with a hose reel visible through the glass door."""
    atlas = Atlas()
    door = p.tile(p.RED, 24, 24, border=p.RED_DARK)
    p.rect(door, 2, 2, 21, 17, p.rgb("#cfe3ea"))
    p.circle(door, 11.5, 9.5, 6.5, p.RED_DARK)
    p.circle(door, 11.5, 9.5, 5.0, p.rgb("#d03030"), ring=2.5)
    p.circle(door, 11.5, 9.5, 1.5, p.METAL)
    p.rect(door, 4, 20, 19, 21, p.WHITE)
    atlas.add("door", door)
    atlas.add("cabinet", p.tile(p.RED, 8, 8, profile="bevel"))
    model = Model("wall")
    model.box("cabinet", (1, 1, 12), (15, 15, 16), "cabinet", north="door")
    model.box("handle", (12.5, 7, 11.5), (13.5, 9, 12), "cabinet")
    return Asset("wandhydrant", "furniture", "Wandhydrant", atlas, {"": model})


def brandschutztuer() -> Asset:
    """Self-closing fire door, two blocks high (frame, door leaf, lever handle, door closer, sign)."""
    atlas = Atlas()
    leaf = p.tile(p.LIGHT_GREY, 14, 31, noise=0.02, seed=19)
    p.rect(leaf, 4, 21, 9, 23, p.WHITE)  # "keep closed" sign
    p.rect(leaf, 5, 22, 8, 22, p.SAFETY_GREEN)
    atlas.add("leaf", leaf)
    atlas.add("frame", p.tile(p.METAL_DARKER, 8, 8, profile="bevel"))
    atlas.add("handle", p.tile(p.METAL, 8, 4, vertical=0.4))
    atlas.add("closer", p.tile(p.METAL_DARK, 8, 4, vertical=0.3))
    model = Model("block")
    model.box("frame_left", (0, 0, 6), (1, 32, 10), "frame")
    model.box("frame_right", (15, 0, 6), (16, 32, 10), "frame")
    model.box("frame_top", (1, 31, 6), (15, 32, 10), "frame")
    model.box("leaf", (1, 0, 7), (15, 31, 9), "frame", north="leaf", south="leaf")
    model.box("handle_rose", (12, 13.5, 6.5), (13, 15.5, 7), "handle")
    model.box("handle_lever", (10, 14.25, 6), (13, 14.75, 6.5), "handle")
    model.box("closer_body", (2, 28.5, 6), (7, 30, 7), "closer")
    model.box("closer_arm", (7, 29, 6.25), (11, 29.5, 6.75), "closer")
    return Asset("brandschutztuer", "furniture", "Brandschutztür", atlas, {"": model})


def holzkeil() -> Asset:
    """Wooden door wedge (a typical hazard: fire doors must not be wedged open)."""
    atlas = Atlas()
    atlas.add("wood", p.tile(p.WOOD, 8, 8, noise=0.08, seed=23))
    atlas.add("end", p.tile(p.WOOD_DARK, 4, 4, noise=0.08, seed=29))
    model = Model("small")
    model.box("step_1", (2, 0, 6), (14, 1, 10), "wood", west="end")
    model.box("step_2", (2, 1, 6), (10, 2, 10), "wood", west="end")
    model.box("step_3", (2, 2, 6), (6, 3, 10), "wood", west="end")
    return Asset("holzkeil", "item", "Holzkeil", atlas, {"": model})


def _warning_label(width: int, height: int) -> "p.Image.Image":
    image = p.stripes(p.SAFETY_YELLOW, p.BLACK, width, height)
    p.rect(image, 2, 2, width - 3, height - 3, p.WHITE)
    cx, cy = width // 2, height // 2
    p.rect(image, cx - 4, cy - 2, cx + 3, cy + 2, p.BLACK)  # battery
    p.rect(image, cx + 4, cy - 1, cx + 4, cy + 1, p.BLACK)
    p.rect(image, cx - 3, cy - 1, cx + 2, cy + 1, p.SAFETY_YELLOW)
    return image


def akku_quarantaenebox() -> Asset:
    """Metal box filled with sand for damaged lithium-ion batteries."""
    atlas = Atlas()
    atlas.add("label", _warning_label(24, 14))
    atlas.add("box", p.tile(p.SAFETY_YELLOW, 8, 8, profile="bevel", noise=0.03, seed=31))
    atlas.add("lid", p.tile(p.rgb("#d8ac00"), 8, 8, vertical=0.2))
    atlas.add("handle", p.tile(p.BLACK, 4, 4))
    model = Model("block")
    model.box("box", (1, 0, 2), (15, 9, 14), "box", north="label")
    model.box("lid", (0.5, 9, 1.5), (15.5, 10.5, 14.5), "lid")
    model.box("handle", (6, 10.5, 7.5), (10, 11.5, 8.5), "handle")
    return Asset("akku_quarantaenebox", "furniture", "Akku-Quarantänebox", atlas, {"": model})


def akku_ladeschrank() -> Asset:
    """Fire-resistant battery charging cabinet with ventilated door."""
    atlas = Atlas()
    door = p.perforated(p.METAL_DARKER, p.BLACK, 26, 28)
    p.overlay(door, _warning_label(14, 10), 6, 3)
    atlas.add("door", door)
    atlas.add("body", p.tile(p.METAL_DARKER, 8, 8, profile="bevel"))
    atlas.add("handle", p.tile(p.SAFETY_YELLOW, 4, 8, vertical=0.3))
    model = Model("block")
    model.box("cabinet", (1, 1, 2), (15, 16, 14), "body", north="door")
    model.box("handle", (12.5, 7, 1.25), (13.5, 11, 2), "handle")
    model.box("foot_left", (1.5, 0, 2.5), (3.5, 1, 13.5), "body")
    model.box("foot_right", (12.5, 0, 2.5), (14.5, 1, 13.5), "body")
    return Asset("akku_ladeschrank", "furniture", "Akku-Ladeschrank", atlas, {"": model})


ALL = [feuerloescher_halter, druckknopfmelder, rauchmelder, sprinkler, wandhydrant, brandschutztuer, holzkeil,
       akku_quarantaenebox, akku_ladeschrank]
