from __future__ import annotations

from .. import paint as p
from ..atlas import Atlas
from ..model import Model
from ..output import Asset

DESK = p.rgb("#b58a5a")
DESK_DARK = p.rgb("#8a6540")
SCREEN_BG = p.rgb("#1d3f8a")
FOLDER_COLORS = (p.RED, p.BLUE, p.SAFETY_YELLOW, p.SAFETY_GREEN, p.ORANGE)
LEAF = p.rgb("#3f8f3a")
LEAF_DARK = p.rgb("#2d6b2a")
POT = p.rgb("#b0583a")


def _wood_atlas(atlas: Atlas) -> None:
    atlas.add("wood", p.tile(DESK, 8, 8, noise=0.05, seed=41))
    atlas.add("wood_dark", p.tile(DESK_DARK, 8, 8, noise=0.05, seed=43))
    atlas.add("metal", p.tile(p.METAL_DARK, 8, 8, vertical=0.3))
    atlas.add("black", p.tile(p.BLACK, 8, 8, vertical=0.15))


def schreibtisch() -> Asset:
    """The monitor faces north (front)."""
    atlas = Atlas()
    _wood_atlas(atlas)
    screen = p.canvas(14, 9)
    for y in range(9):
        p.rect(screen, 0, y, 13, y, tuple(int(c * (0.75 + 0.3 * y / 8)) for c in SCREEN_BG))
    p.rect(screen, 2, 2, 8, 2, p.WHITE)
    p.rect(screen, 2, 4, 11, 4, p.rgb("#9fb8e8"))
    p.rect(screen, 2, 6, 6, 6, p.rgb("#9fb8e8"))
    atlas.add("screen", screen)
    keys = p.tile(p.PLASTIC_LIGHT, 10, 4)
    for x in range(1, 9, 2):
        p.rect(keys, x, 1, x, 2, p.PLASTIC)
    atlas.add("keys", keys)
    model = Model("block")
    model.box("top", (0, 11, 2), (16, 12.5, 14), "wood")
    model.box("panel", (1, 2, 12), (15, 11, 13.5), "wood_dark")
    model.box("leg_left", (1, 0, 3), (2.5, 11, 13), "wood_dark")
    model.box("leg_right", (13.5, 0, 3), (15, 11, 13), "wood_dark")
    model.box("monitor_foot", (6, 12.5, 9), (10, 13, 12), "black")
    model.box("monitor_pole", (7.5, 13, 10.5), (8.5, 15, 11.5), "black")
    model.box("monitor", (1, 14.5, 10.5), (15, 23.5, 11.5), "black", north="screen")
    model.box("keyboard", (3, 12.5, 4), (13, 13, 8), "black", up="keys")
    return Asset("schreibtisch", "furniture", "Schreibtisch mit Monitor", atlas, {"": model})


def buerostuhl() -> Asset:
    atlas = Atlas()
    atlas.add("fabric", p.tile(p.rgb("#2f4f6f"), 8, 8, noise=0.05, seed=47))
    atlas.add("metal", p.tile(p.METAL_DARK, 8, 8, vertical=0.3))
    atlas.add("black", p.tile(p.BLACK, 8, 8, vertical=0.15))
    model = Model("block")
    model.box("seat", (4, 7, 4), (12, 9, 12), "fabric")
    model.box("back", (4, 9, 11), (12, 18, 12.5), "fabric")
    model.box("pole", (7.5, 2, 7.5), (8.5, 7, 8.5), "metal")
    model.box("base_x", (2, 0.5, 7.25), (14, 1.5, 8.75), "black")
    model.box("base_z", (7.25, 0.5, 2), (8.75, 1.5, 14), "black")
    model.box("arm_left", (3, 9, 5), (4, 12, 11), "black")
    model.box("arm_right", (12, 9, 5), (13, 12, 11), "black")
    return Asset("buerostuhl", "furniture", "Bürostuhl", atlas, {"": model})


def aktenschrank() -> Asset:
    atlas = Atlas()
    atlas.add("body", p.tile(p.LIGHT_GREY, 8, 8, profile="bevel"))
    front = p.tile(p.LIGHT_GREY, 12, 20)
    for y in (4, 14):
        p.rect(front, 1, y, 10, y, p.METAL_DARK)
        p.rect(front, 4, y + 3, 7, y + 4, p.METAL_DARK)
    atlas.add("front", front)
    model = Model("block")
    model.box("cabinet", (3, 0, 4), (13, 20, 13), "body", north="front")
    return Asset("aktenschrank", "furniture", "Aktenschrank", atlas, {"": model})


def regal() -> Asset:
    atlas = Atlas()
    _wood_atlas(atlas)
    for i, color in enumerate(FOLDER_COLORS):
        atlas.add(f"folder{i}", p.tile(color, 4, 8, vertical=0.2, border=tuple(int(c * 0.7) for c in color)))
    model = Model("block")
    for y in (0, 10, 20):
        model.box(f"board{y}", (1, y, 3), (15, y + 1, 13), "wood")
    model.box("side_left", (0, 0, 3), (1, 31, 13), "wood_dark")
    model.box("side_right", (15, 0, 3), (16, 31, 13), "wood_dark")
    model.box("back", (1, 0, 12.5), (15, 31, 13), "wood_dark")
    model.box("top", (0, 30, 3), (16, 31, 13), "wood")
    for row, y in ((0, 1), (1, 11), (2, 21)):
        x = 2.0
        for i in range(5):
            color = (i + row) % len(FOLDER_COLORS)
            model.box(f"folder_{row}_{i}", (x, y, 5), (x + 2, y + 8, 12), f"folder{color}")
            x += 2.5
    return Asset("regal", "furniture", "Regal mit Ordnern", atlas, {"": model})


def pflanze() -> Asset:
    atlas = Atlas()
    atlas.add("pot", p.tile(POT, 8, 8, profile="cylinder"))
    atlas.add("soil", p.tile(p.rgb("#4a3321"), 8, 8, noise=0.1, seed=53))
    atlas.add("leaf", p.tile(LEAF, 8, 8, noise=0.12, seed=59))
    atlas.add("leaf_dark", p.tile(LEAF_DARK, 8, 8, noise=0.12, seed=61))
    model = Model("block")
    model.box("pot", (5, 0, 5), (11, 6, 11), "pot", up="soil")
    model.box("stem", (7.5, 6, 7.5), (8.5, 14, 8.5), "leaf_dark")
    model.box("leaves_a", (3, 10, 5), (13, 15, 11), "leaf")
    model.box("leaves_b", (5, 12, 3), (11, 18, 13), "leaf_dark")
    model.box("leaves_c", (4, 15, 4), (12, 21, 12), "leaf")
    return Asset("pflanze", "furniture", "Topfpflanze", atlas, {"": model})


def drucker() -> Asset:
    atlas = Atlas()
    atlas.add("body", p.tile(p.OFF_WHITE, 8, 8, profile="bevel"))
    atlas.add("dark", p.tile(p.PLASTIC, 8, 8, vertical=0.2))
    atlas.add("paper", p.tile(p.WHITE, 8, 8))
    model = Model("block")
    model.box("body", (2, 0, 3), (14, 7, 13), "body")
    model.box("lid", (3, 7, 4), (13, 8, 12), "dark")
    model.box("tray", (4, 3, 0.5), (12, 3.5, 3), "dark", up="paper")
    model.box("panel", (9, 8, 5), (13, 8.5, 7), "dark")
    return Asset("drucker", "furniture", "Drucker", atlas, {"": model})


def whiteboard() -> Asset:
    atlas = Atlas()
    board = p.tile(p.WHITE, 28, 18)
    p.outline(board, 0, 0, 27, 17, p.METAL)
    for i, (x0, x1, y) in enumerate(((3, 14, 4), (3, 20, 7), (3, 10, 10), (16, 24, 10), (3, 18, 13))):
        p.rect(board, x0, y, x1, y, (p.BLUE, p.RED, p.SAFETY_GREEN, p.BLUE, p.BLACK)[i % 5])
    atlas.add("board", board)
    atlas.add("frame", p.tile(p.METAL, 8, 4, vertical=0.3))
    model = Model("wall")
    model.box("board", (1, 3, 15), (15, 12, 16), "frame", north="board")
    model.box("tray", (3, 2.5, 14), (13, 3, 15), "frame")
    return Asset("whiteboard", "furniture", "Whiteboard", atlas, {"": model})


def wasserspender() -> Asset:
    atlas = Atlas()
    atlas.add("body", p.tile(p.OFF_WHITE, 8, 8, profile="bevel"))
    atlas.add("water", p.tile(p.rgb("#7fc8ea"), 8, 8, profile="cylinder"))
    atlas.add("dark", p.tile(p.PLASTIC, 8, 8))
    model = Model("block")
    model.box("body", (4, 0, 4), (12, 16, 12), "body")
    model.box("bottle", (5, 16, 5), (11, 26, 11), "water")
    model.box("bottle_x", (4.5, 17, 6), (11.5, 25, 10), "water")
    model.box("tap", (7, 11, 2.5), (9, 12.5, 4), "dark")
    return Asset("wasserspender", "furniture", "Wasserspender", atlas, {"": model})


def werkbank() -> Asset:
    atlas = Atlas()
    _wood_atlas(atlas)
    atlas.add("steel", p.tile(p.METAL, 8, 8, vertical=0.3))
    model = Model("block")
    model.box("top", (0, 12, 1), (16, 14, 15), "wood")
    model.box("leg_fl", (1, 0, 2), (3, 12, 4), "metal")
    model.box("leg_fr", (13, 0, 2), (15, 12, 4), "metal")
    model.box("leg_bl", (1, 0, 12), (3, 12, 14), "metal")
    model.box("leg_br", (13, 0, 12), (15, 12, 14), "metal")
    model.box("shelf", (1, 4, 3), (15, 5, 13), "wood_dark")
    model.box("vise", (11, 14, 1.5), (15, 17, 5), "steel")
    model.box("vise_handle", (12.5, 15, 0), (13.5, 16, 1.5), "steel")
    return Asset("werkbank", "furniture", "Werkbank", atlas, {"": model})


def gasbrenner() -> Asset:
    """The valve is a separate lever block in the map."""
    atlas = Atlas()
    atlas.add("bottle", p.tile(p.rgb("#c7442e"), 8, 8, profile="cylinder"))
    atlas.add("steel", p.tile(p.METAL, 8, 8, vertical=0.3))
    atlas.add("dark", p.tile(p.METAL_DARKER, 8, 8, vertical=0.3))
    atlas.add("hose", p.tile(p.BLACK, 8, 8, profile="cylinder"))
    model = Model("block")
    model.box("bottle", (1, 0, 5), (7, 14, 11), "bottle", up="steel")
    model.box("bottle_x", (0.5, 0, 6), (7.5, 14, 10), "bottle", up="steel")
    model.box("bottle_neck", (3, 14, 7), (5, 16, 9), "steel")
    model.box("bottle_valve", (2.5, 16, 7.5), (5.5, 17, 8.5), "dark")
    model.box("burner_foot", (9, 0, 5), (15, 1, 11), "dark")
    model.box("burner_tube", (11.25, 1, 7.25), (12.75, 9, 8.75), "steel")
    model.box("burner_collar", (10.75, 2, 6.75), (13.25, 3.5, 9.25), "dark")
    model.box("hose", (5.5, 2, 7.5), (11.25, 3, 8.5), "hose")
    return Asset("gasbrenner", "furniture", "Gasbrenner mit Flasche", atlas, {"": model})


def metalltonne() -> Asset:
    atlas = Atlas()
    atlas.add("side", p.tile(p.METAL_DARK, 12, 16, profile="cylinder", noise=0.04, seed=67))
    atlas.add("rim", p.tile(p.METAL_DARKER, 8, 8))
    atlas.add("paper", p.tile(p.OFF_WHITE, 8, 8, noise=0.08, seed=71))
    model = Model("block")
    model.box("body", (3, 0, 3), (13, 14, 13), "side", up="paper", down="rim")
    model.box("body_x", (2.5, 0, 4), (13.5, 14, 12), "side", up="paper", down="rim")
    model.box("body_z", (4, 0, 2.5), (12, 14, 13.5), "side", up="paper", down="rim")
    model.box("rim", (2.5, 13, 2.5), (13.5, 14.5, 13.5), "rim", up=None)
    return Asset("metalltonne", "furniture", "Metalltonne", atlas, {"": model})


def palette() -> Asset:
    atlas = Atlas()
    atlas.add("slat", p.tile(p.WOOD, 8, 8, noise=0.1, seed=73))
    atlas.add("slat_dark", p.tile(p.WOOD_DARK, 8, 8, noise=0.1, seed=79))
    model = Model("block")
    for i, x in enumerate((0, 6.5, 13)):
        model.box(f"top{i}", (x, 2, 0), (x + 3, 2.5, 16), "slat")
    for i, z in enumerate((0, 7, 14)):
        model.box(f"bottom{i}", (0, 0, z), (16, 0.5, z + 2), "slat_dark")
        for j, x in enumerate((0, 6.5, 13)):
            model.box(f"block{i}{j}", (x, 0.5, z), (x + 3, 2, z + 2), "slat_dark")
    return Asset("palette", "furniture", "Holzpalette", atlas, {"": model})


def werkzeugwand() -> Asset:
    atlas = Atlas()
    board = p.perforated(p.rgb("#6b4e2e"), p.rgb("#4a3520"), 28, 20, step=3)
    for x0, y0, x1, y1, color in ((3, 3, 4, 12, p.METAL), (3, 12, 5, 14, p.METAL_DARK),
                                  (9, 3, 10, 13, p.RED), (8, 13, 11, 15, p.METAL),
                                  (15, 4, 22, 5, p.METAL), (14, 5, 16, 11, p.RED),
                                  (24, 3, 25, 14, p.SAFETY_YELLOW)):
        p.rect(board, x0, y0, x1, y1, color)
    atlas.add("board", board)
    atlas.add("edge", p.tile(p.WOOD_DARK, 4, 4))
    model = Model("wall")
    model.box("board", (1, 2, 15), (15, 12, 16), "edge", north="board")
    return Asset("werkzeugwand", "furniture", "Werkzeugwand", atlas, {"": model})


def haengeschrank() -> Asset:
    atlas = Atlas()
    front = p.tile(p.WHITE, 24, 12, profile="bevel")
    p.rect(front, 11, 1, 12, 10, p.OFF_WHITE)
    p.rect(front, 9, 7, 9, 9, p.METAL_DARK)
    p.rect(front, 14, 7, 14, 9, p.METAL_DARK)
    atlas.add("front", front)
    atlas.add("body", p.tile(p.WHITE, 8, 8, profile="bevel"))
    model = Model("wall")
    model.box("cabinet", (0, 2, 10), (16, 13, 16), "body", north="front")
    return Asset("haengeschrank", "furniture", "Hängeschrank", atlas, {"": model})


def spuele() -> Asset:
    atlas = Atlas()
    atlas.add("counter", p.tile(p.WHITE, 8, 8, profile="bevel"))
    top = p.tile(p.METAL_LIGHT, 16, 16)
    p.rect(top, 3, 3, 12, 12, p.METAL_DARK)
    p.rect(top, 4, 4, 11, 11, p.METAL)
    atlas.add("top", top)
    atlas.add("steel", p.tile(p.METAL, 8, 8, vertical=0.3))
    model = Model("block")
    model.box("counter", (0, 0, 0), (16, 13, 16), "counter", up="top")
    model.box("tap_base", (7, 13, 12.5), (9, 17, 14.5), "steel")
    model.box("tap_arm", (7.5, 17, 7), (8.5, 18, 14.5), "steel")
    model.box("tap_spout", (7.5, 15.5, 7), (8.5, 17, 8), "steel")
    return Asset("spuele", "furniture", "Spüle", atlas, {"": model})


def esstisch() -> Asset:
    atlas = Atlas()
    _wood_atlas(atlas)
    model = Model("block")
    model.box("top", (0, 12, 0), (16, 13.5, 16), "wood")
    for name, (x, z) in (("fl", (1, 1)), ("fr", (13, 1)), ("bl", (1, 13)), ("br", (13, 13))):
        model.box(f"leg_{name}", (x, 0, z), (x + 2, 12, z + 2), "wood_dark")
    return Asset("esstisch", "furniture", "Esstisch", atlas, {"": model})


def bank() -> Asset:
    atlas = Atlas()
    _wood_atlas(atlas)
    model = Model("block")
    model.box("seat", (0, 7, 4), (16, 8.5, 12), "wood")
    model.box("leg_left", (1, 0, 5), (3, 7, 11), "wood_dark")
    model.box("leg_right", (13, 0, 5), (15, 7, 11), "wood_dark")
    return Asset("bank", "furniture", "Sitzbank", atlas, {"": model})


def deckenlampe() -> Asset:
    atlas = Atlas()
    atlas.add("frame", p.tile(p.METAL_LIGHT, 8, 8, vertical=0.2))
    atlas.add("light", p.tile(p.rgb("#fff6d5"), 8, 8))
    model = Model("block")
    model.box("panel", (1, 14.5, 1), (15, 16, 15), "frame", down="light")
    return Asset("deckenlampe", "furniture", "Deckenlampe", atlas, {"": model})


ALL = [schreibtisch, buerostuhl, aktenschrank, regal, pflanze, drucker, whiteboard, wasserspender, werkbank,
       gasbrenner, metalltonne, palette, werkzeugwand, haengeschrank, spuele, esstisch, bank, deckenlampe]
