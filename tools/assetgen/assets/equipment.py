"""Personal equipment and tools: fire blanket, pot lid, phone, clipboard, high-visibility vest, ID card."""

from __future__ import annotations

from .. import paint as p
from ..atlas import Atlas
from ..model import Model
from ..output import Asset

DIGITS = {
    "1": [".W", "WW", ".W", ".W", ".W"],
    "2": ["WW.", "..W", ".W.", "W..", "WWW"],
}


def loeschdecke() -> Asset:
    """Fire blanket in its wall pouch with two pull tapes."""
    atlas = Atlas()
    front = p.tile(p.RED, 10, 13, border=p.RED_DARK)
    p.rect(front, 2, 2, 7, 7, p.WHITE)
    p.rect(front, 3, 5, 6, 6, p.RED)  # stylized folded blanket
    p.rect(front, 4, 3, 5, 4, p.ORANGE)  # small flame above
    for x in range(2, 8):
        if x % 2 == 0:
            front.putpixel((x, 10), p.shade(p.WHITE))
    atlas.add("front", front)
    atlas.add("pouch", p.tile(p.RED, 8, 8, profile="bevel", noise=0.04, seed=5))
    atlas.add("strap", p.tile(p.BLACK, 4, 8, vertical=0.2))
    model = Model("wall")
    model.box("pouch", (3, 2, 13), (13, 15, 16), "pouch", north="front")
    model.box("tape_left", (4.5, 0, 13.5), (6, 2.5, 14.5), "strap")
    model.box("tape_right", (10, 0, 13.5), (11.5, 2.5, 14.5), "strap")
    return Asset("loeschdecke", "item", "Löschdecke", atlas, {"": model})


def topfdeckel() -> Asset:
    atlas = Atlas()
    lid = p.tile(p.METAL_LIGHT, 12, 12)
    p.circle(lid, 5.5, 5.5, 6.0, p.METAL, ring=1.0)
    atlas.add("lid", lid)
    atlas.add("rim", p.tile(p.METAL, 8, 4, vertical=0.3))
    atlas.add("knob", p.tile(p.BLACK, 4, 4, vertical=0.3))
    model = Model("small")
    model.box("lid_x", (2, 0, 4), (14, 1, 12), "rim", up="lid", down="lid")
    model.box("lid_z", (4, 0, 2), (12, 1, 14), "rim", up="lid", down="lid")
    model.box("dome", (3.5, 1, 3.5), (12.5, 1.5, 12.5), "rim", up="lid")
    model.box("knob", (7, 1.5, 7), (9, 3, 9), "knob")
    return Asset("topfdeckel", "item", "Topfdeckel", atlas, {"": model})


def handy() -> Asset:
    """Smartphone showing the emergency number 112 (front = north face)."""
    atlas = Atlas()
    screen = p.canvas(12, 20)
    for y in range(20):
        p.rect(screen, 0, y, 11, y, tuple(int(c * (0.7 + 0.3 * y / 19)) for c in p.rgb("#1d3f8a")))
    digits = p.grid(DIGITS["1"], {"W": p.WHITE})
    p.overlay(screen, digits, 2, 5)
    p.overlay(screen, digits, 5, 5)
    p.overlay(screen, p.grid(DIGITS["2"], {"W": p.WHITE}), 8, 5)
    p.circle(screen, 5.5, 14.5, 2.2, p.rgb("#2ec24f"))  # green call button
    p.outline(screen, 0, 0, 11, 19, p.BLACK)
    atlas.add("screen", screen)
    atlas.add("case", p.tile(p.BLACK, 8, 8, vertical=0.15))
    model = Model("flat")
    model.box("phone", (5.5, 0, 7.5), (10.5, 10, 8.5), "case", north="screen")
    return Asset("handy", "item", "Handy (Notruf 112)", atlas, {"": model})


def klemmbrett() -> Asset:
    atlas = Atlas()
    paper = p.tile(p.WHITE, 18, 22)
    for row in range(3, 21, 3):
        p.rect(paper, 2, row, 3, row + 1, p.INK)  # checkbox
        p.rect(paper, 5, row, 15, row, p.GREY)  # line
    for x, y in ((2, 3), (3, 4), (2, 6), (3, 7)):
        paper.putpixel((x, y), p.shade(p.rgb("#2e9b45")))  # ticked boxes
    atlas.add("paper", paper)
    atlas.add("board", p.tile(p.WOOD, 8, 8, noise=0.06, seed=7))
    atlas.add("clip", p.tile(p.METAL, 8, 4, vertical=0.4))
    model = Model("flat")
    model.box("board", (3, 0, 7.75), (13, 14, 8.25), "board")
    model.box("paper", (3.5, 0.5, 7.5), (12.5, 12, 7.75), "paper", south=None)
    model.box("clip", (6, 12, 7.25), (10, 14.5, 7.75), "clip")
    return Asset("klemmbrett", "item", "Klemmbrett mit Checkliste", atlas, {"": model})


def warnweste() -> Asset:
    """Folded high-visibility vest (worn version: equippable component, later)."""
    atlas = Atlas()
    top = p.tile(p.HI_VIS, 12, 12, noise=0.04, seed=9)
    p.rect(top, 0, 3, 11, 4, p.rgb("#c9ccd0"))
    p.rect(top, 0, 8, 11, 9, p.rgb("#c9ccd0"))
    atlas.add("top", top)
    atlas.add("side", p.tile(p.HI_VIS, 8, 4, vertical=0.2))
    model = Model("small")
    model.box("vest", (3, 0, 4), (13, 2, 12), "side", up="top")
    return Asset("warnweste", "item", "Warnweste (gefaltet)", atlas, {"": model})


def brandschutzhelfer_ausweis() -> Asset:
    atlas = Atlas()
    card = p.tile(p.WHITE, 16, 11)
    p.rect(card, 0, 0, 15, 2, p.RED)
    p.rect(card, 1, 4, 5, 9, p.GREY)  # photo
    for row in (4, 6, 8):
        p.rect(card, 7, row, 14, row, p.INK)
    atlas.add("card", card)
    atlas.add("edge", p.tile(p.OFF_WHITE, 4, 4))
    model = Model("flat")
    model.box("card", (4, 0, 7.75), (12, 5.5, 8), "edge", north="card")
    return Asset("brandschutzhelfer_ausweis", "item", "Brandschutzhelfer-Ausweis", atlas, {"": model})


ALL = [loeschdecke, topfdeckel, handy, klemmbrett, warnweste, brandschutzhelfer_ausweis]
