"""Electrical hazards: batteries (intact, damaged, swollen), power banks, chargers, power strip, damaged cable."""

from __future__ import annotations

from .. import paint as p
from ..atlas import Atlas
from ..model import Model
from ..output import Asset

SCORCH = p.rgb("#3a2a20")


def _battery_front(damaged: bool) -> "p.Image.Image":
    front = p.tile(p.PLASTIC, 12, 26, profile="bevel")
    p.rect(front, 2, 3, 9, 6, p.METAL_DARKER)
    for i, x in enumerate(range(3, 9, 2)):  # charge level LEDs
        p.rect(front, x, 4, x, 5, p.rgb("#3cdc5a") if i < (1 if damaged else 3) else p.METAL_DARK)
    p.rect(front, 2, 10, 9, 15, p.SAFETY_YELLOW)
    p.rect(front, 3, 12, 8, 13, p.BLACK)  # warning label with battery symbol
    if damaged:
        for x, y in ((8, 17), (7, 18), (7, 19), (6, 20), (5, 21), (5, 22), (9, 18), (10, 19)):
            front.putpixel((x, y), p.shade(p.rgb("#c8c8c8")))  # crack
        p.circle(front, 4, 21, 2.5, SCORCH)
    return front


def _battery(asset_id: str, title: str, state: str) -> Asset:
    """E-bike style battery pack. state: ok | damaged | swollen."""
    atlas = Atlas()
    atlas.add("front", _battery_front(state != "ok"))
    atlas.add("shell", p.tile(p.PLASTIC, 8, 8, profile="bevel", noise=0.03, seed=11))
    atlas.add("connector", p.tile(p.METAL_DARKER, 8, 4, vertical=0.3))
    model = Model("small")
    model.box("pack", (5, 0, 6), (11, 13, 10), "shell", north="front")
    model.box("connector", (6, 13, 6.5), (10, 14, 9.5), "connector")
    if state == "swollen":
        model.box("bulge", (4.5, 3, 5.5), (11.5, 10, 10.5), "shell", north="front")
        model.box("bulge_core", (4.25, 5, 5.25), (11.75, 8, 10.75), "shell")
    elif state == "damaged":
        model.box("broken_corner", (10, 10.5, 5.5), (11.5, 13.5, 7), "shell", rotation=("y", 22.5, (10.75, 12, 6.25)))
    return Asset(asset_id, "item", title, atlas, {"": model})


def akku() -> Asset:
    return _battery("akku", "Akku (intakt)", "ok")


def akku_beschaedigt() -> Asset:
    return _battery("akku_beschaedigt", "Akku (beschädigt)", "damaged")


def akku_geblaeht() -> Asset:
    return _battery("akku_geblaeht", "Akku (aufgebläht)", "swollen")


def _powerbank(asset_id: str, title: str, swollen: bool) -> Asset:
    atlas = Atlas()
    front = p.tile(p.rgb("#d8dadc"), 12, 20, profile="bevel")
    for i, y in enumerate(range(14, 18)):
        p.rect(front, 5, y, 6, y, p.rgb("#3c8cff") if i >= 1 else p.METAL)
    atlas.add("front", front)
    atlas.add("shell", p.tile(p.rgb("#c8cacc"), 8, 8, vertical=0.15))
    atlas.add("port", p.tile(p.BLACK, 4, 4))
    model = Model("flat")
    model.box("body", (5, 0, 7), (11, 10, 9), "shell", north="front")
    model.box("port", (7, 10, 7.5), (9, 10.5, 8.5), "port")
    if swollen:
        model.box("bulge", (4.5, 1.5, 6.25), (11.5, 8.5, 9.75), "shell", north="front")
    return Asset(asset_id, "item", title, atlas, {"": model})


def powerbank() -> Asset:
    return _powerbank("powerbank", "Powerbank", False)


def powerbank_geblaeht() -> Asset:
    return _powerbank("powerbank_geblaeht", "Powerbank (aufgebläht)", True)


def _charger(asset_id: str, title: str, cheap: bool) -> Asset:
    color = p.BLACK if cheap else p.WHITE
    atlas = Atlas()
    front = p.tile(color, 8, 10, profile="bevel")
    if not cheap:  # conformity marks and a lightning symbol on the original charger
        for x, y in ((2, 2), (3, 3), (2, 4), (3, 5), (4, 5)):
            front.putpixel((x, y), p.shade(p.INK))
        p.rect(front, 5, 7, 6, 7, p.INK)
    atlas.add("front", front)
    atlas.add("shell", p.tile(color, 8, 8, vertical=0.15))
    atlas.add("pin", p.tile(p.METAL, 4, 4, vertical=0.3))
    atlas.add("cable", p.tile(p.BLACK if cheap else p.OFF_WHITE, 8, 4, profile="cylinder"))
    atlas.add("copper", p.tile(p.COPPER, 4, 4, noise=0.15, seed=13))
    model = Model("small")
    model.box("brick", (6, 0, 6), (10, 5, 10), "shell", north="front")
    model.box("pin_left", (6.9, 5, 7.7), (7.5, 7, 8.3), "pin")
    model.box("pin_right", (8.5, 5, 7.7), (9.1, 7, 8.3), "pin")
    model.box("cable", (10, 1, 7.5), (14, 2, 8.5), "cable")
    if cheap:  # frayed cable end with exposed copper
        model.box("frayed", (14, 1.15, 7.65), (15.25, 1.85, 8.35), "copper")
    return Asset(asset_id, "item", title, atlas, {"": model})


def ladegeraet() -> Asset:
    return _charger("ladegeraet", "Ladegerät (Original)", False)


def ladegeraet_billig() -> Asset:
    return _charger("ladegeraet_billig", "Ladegerät (Billig, beschädigt)", True)


def mehrfachsteckdose() -> Asset:
    atlas = Atlas()
    top = p.tile(p.WHITE, 24, 8, border=p.OFF_WHITE)
    for cx in (3.5, 9.5, 15.5):
        p.circle(top, cx, 3.5, 2.6, p.OFF_WHITE)
        top.putpixel((int(cx) - 1, 3), p.shade(p.BLACK))
        top.putpixel((int(cx) + 1, 3), p.shade(p.BLACK))
    p.rect(top, 20, 2, 22, 5, p.RED)  # illuminated switch
    atlas.add("top", top)
    atlas.add("shell", p.tile(p.WHITE, 8, 4, vertical=0.2))
    atlas.add("cable", p.tile(p.OFF_WHITE, 8, 4, profile="cylinder"))
    model = Model("small")
    model.box("strip", (2, 0, 6.5), (14, 2.5, 9.5), "shell", up="top")
    model.box("cable", (-1, 0.75, 7.5), (2, 1.75, 8.5), "cable")
    return Asset("mehrfachsteckdose", "item", "Mehrfachsteckdose", atlas, {"": model})


def kabel_beschaedigt() -> Asset:
    atlas = Atlas()
    atlas.add("insulation", p.tile(p.BLACK, 8, 4, profile="cylinder"))
    atlas.add("copper", p.tile(p.COPPER, 8, 4, noise=0.15, seed=17))
    atlas.add("blue", p.tile(p.BLUE, 4, 4))
    atlas.add("brown", p.tile(p.WOOD_DARK, 4, 4))
    model = Model("small")
    model.box("cable_left", (0, 0, 7.25), (6, 1.5, 8.75), "insulation")
    model.box("wire_blue", (6, 0.3, 7.4), (10, 0.9, 7.95), "blue")
    model.box("wire_brown", (6, 0.6, 8.05), (10, 1.2, 8.6), "brown")
    model.box("copper", (7.5, 0.25, 7.6), (8.5, 1.25, 8.4), "copper")
    model.box("cable_right", (10, 0, 7.25), (16, 1.5, 8.75), "insulation")
    return Asset("kabel_beschaedigt", "item", "Kabel (beschädigte Isolierung)", atlas, {"": model})


ALL = [akku, akku_beschaedigt, akku_geblaeht, powerbank, powerbank_geblaeht, ladegeraet, ladegeraet_billig,
       mehrfachsteckdose, kabel_beschaedigt]
