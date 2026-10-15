"""Kitchen and office appliances: stove with oil pot, deep fryer, fan heater, coffee machine."""

from __future__ import annotations

from .. import paint as p
from ..atlas import Atlas
from ..model import Model
from ..output import Asset

STEEL = p.rgb("#c4c8cc")


def herd() -> Asset:
    """Stove with ceramic cooktop and a pot of cooking oil (scenario: grease fire)."""
    atlas = Atlas()
    cooktop = p.tile(p.BLACK, 16, 14)
    for cx, cy, r in ((4, 4, 3.2), (11, 4, 2.6), (4, 10, 2.6), (11, 10, 3.2)):
        p.circle(cooktop, cx, cy, r, p.rgb("#5a1a14"), ring=1.0)
    atlas.add("cooktop", cooktop)
    front = p.tile(p.WHITE, 16, 14)
    p.rect(front, 2, 4, 13, 11, p.BLACK)
    p.rect(front, 3, 5, 12, 10, p.rgb("#3a3a40"))  # oven window
    p.rect(front, 3, 2, 12, 2, STEEL)  # handle shadow line
    for x in (2, 5, 10, 13):
        front.putpixel((x, 0), p.shade(p.BLACK))  # knobs
    atlas.add("front", front)
    atlas.add("body", p.tile(p.WHITE, 8, 8, profile="bevel"))
    atlas.add("steel", p.tile(STEEL, 8, 8, vertical=0.3))
    oil = p.tile(p.OIL, 8, 8, noise=0.05, seed=37)
    p.circle(oil, 3.5, 3.5, 4.2, STEEL, ring=0.8)
    atlas.add("oil", oil)
    model = Model("block")
    model.box("body", (0, 0, 1), (16, 14, 15), "body", north="front")
    model.box("cooktop", (0, 14, 1), (16, 14.5, 15), "body", up="cooktop")
    model.box("oven_handle", (3, 11, 0.25), (13, 11.75, 1), "steel")
    model.box("pot", (2, 14.5, 2), (8, 19, 8), "steel", up="oil")
    model.box("pot_handle_left", (0.75, 17.5, 4.5), (2, 18.25, 5.5), "steel")
    model.box("pot_handle_right", (8, 17.5, 4.5), (9.25, 18.25, 5.5), "steel")
    return Asset("herd", "furniture", "Herd mit Topf (Öl)", atlas, {"": model})


def fritteuse() -> Asset:
    atlas = Atlas()
    oil = p.tile(p.OIL, 10, 8, noise=0.06, seed=41)
    p.outline(oil, 0, 0, 9, 7, STEEL)
    atlas.add("oil", oil)
    atlas.add("body", p.tile(STEEL, 8, 8, profile="bevel"))
    atlas.add("basket", p.perforated(STEEL, p.METAL_DARK, 8, 4))
    atlas.add("handle", p.tile(p.BLACK, 4, 4))
    front = p.tile(STEEL, 12, 8, profile="bevel")
    p.circle(front, 3, 4, 1.5, p.BLACK)  # thermostat knob
    front.putpixel((8, 3), p.shade(p.rgb("#ff8c1a")))  # heating LED
    atlas.add("front", front)
    model = Model("block")
    model.box("body", (2, 0, 3), (14, 8, 13), "body", north="front", up="oil")
    model.box("basket_rim", (3, 7.5, 4), (13, 8.5, 12), "basket", up=None)
    model.box("basket_handle", (7, 8, 0.5), (9, 9, 4), "handle")
    return Asset("fritteuse", "furniture", "Fritteuse", atlas, {"": model})


def heizluefter() -> Asset:
    """Fan heater (hazard: paper too close to the heater)."""
    atlas = Atlas()
    grille = p.tile(p.BLACK, 8, 10)
    for y in range(1, 9, 2):
        p.rect(grille, 1, y, 6, y, p.rgb("#ff7a1a"))
    atlas.add("grille", grille)
    atlas.add("body", p.tile(p.OFF_WHITE, 8, 8, profile="bevel"))
    atlas.add("feet", p.tile(p.PLASTIC, 4, 4))
    atlas.add("knob", p.tile(p.PLASTIC_LIGHT, 4, 4))
    model = Model("block")
    model.box("body", (4, 1, 5), (12, 11, 11), "body", north="grille")
    model.box("feet", (4.5, 0, 5.5), (11.5, 1, 10.5), "feet")
    model.box("knob", (9.5, 11, 8), (10.5, 11.75, 9), "knob")
    return Asset("heizluefter", "furniture", "Heizlüfter", atlas, {"": model})


def kaffeemaschine() -> Asset:
    atlas = Atlas()
    carafe = p.tile(p.GLASS, 8, 8, vertical=0.2)
    p.rect(carafe, 0, 4, 7, 7, p.rgb("#4a2a14"))  # coffee
    atlas.add("carafe", carafe)
    atlas.add("body", p.tile(p.BLACK, 8, 8, profile="bevel"))
    atlas.add("tank", p.tile(p.rgb("#7fb8d8"), 8, 8, vertical=0.2))
    switch = p.tile(p.BLACK, 4, 4)
    switch.putpixel((1, 1), p.shade(p.rgb("#ff3030")))
    atlas.add("switch", switch)
    model = Model("block")
    model.box("base", (3, 0, 2), (13, 1.5, 13), "body")
    model.box("tower", (3, 1.5, 9), (13, 15, 13), "body", north="body")
    model.box("hood", (3, 12, 2), (13, 15, 9), "body")
    model.box("tank", (4, 2, 13), (12, 13, 14.5), "tank")
    model.box("carafe", (5, 1.5, 3.5), (11, 8, 8.5), "carafe")
    model.box("switch", (11, 1.5, 1.75), (12, 2.5, 2), "switch")
    return Asset("kaffeemaschine", "furniture", "Kaffeemaschine", atlas, {"": model})


ALL = [herd, fritteuse, heizluefter, kaffeemaschine]
