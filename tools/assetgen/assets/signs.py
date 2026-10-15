"""Safety signs (own pixel-art pictograms modeled after the standardized signs) and an evacuation plan.

Green = escape and rescue signs, red = fire protection signs. Signs are thin wall plates whose
front is the north face.
"""

from __future__ import annotations

from .. import paint as p
from ..atlas import Atlas
from ..model import Model
from ..output import Asset

W = {"W": p.WHITE}

RUNNING_PERSON = [
    "......WW..",
    "......WW..",
    "....WWW...",
    "..WW.WWW..",
    ".W...WW.W.",
    ".....WW...",
    "....WW.W..",
    "...W....W.",
    "..W.....W.",
    ".W.......W",
    "W.........",
]
ARROW_RIGHT = [
    "....W...",
    "....WW..",
    "WWWWWWW.",
    "WWWWWWWW",
    "WWWWWWW.",
    "....WW..",
    "....W...",
]
PERSON = [
    ".W.",
    "WWW",
    ".W.",
    "W.W",
    "W.W",
]
EXTINGUISHER = [
    "..WWWW...",
    "..W.W....",
    ".W.WW....",
    ".W.WWW...",
    "W.WWWWW..",
    "W.WWWWW..",
    "..WWWWW..",
    "..WWWWW..",
    "..WWWWW..",
    "..WWWWW..",
    "...WWW...",
]
FLAME = [
    ".W.",
    "WW.",
    "WWW",
    ".W.",
]
HAND = [
    "..W...",
    "..W...",
    ".WWWW.",
    "WWWWWW",
    "WWWWWW",
    ".WWWW.",
]


def _plate(background: p.RGB, width: int = 16, height: int = 16) -> "p.Image.Image":
    image = p.tile(background, width, height)
    p.outline(image, 1, 1, width - 2, height - 2, p.WHITE)
    return image


def _sign(asset_id: str, title: str, face: "p.Image.Image", wide: bool = False) -> Asset:
    atlas = Atlas()
    atlas.add("face", face)
    atlas.add("edge", p.tile(p.OFF_WHITE, 4, 4))
    model = Model("wall")
    if wide:
        model.box("plate", (0, 4, 15.25), (16, 12, 16), "edge", north="face")
    else:
        model.box("plate", (1, 1, 15.25), (15, 15, 16), "edge", north="face")
    return Asset(asset_id, "sign", title, atlas, {"": model})


def schild_notausgang() -> Asset:
    face = _plate(p.SAFETY_GREEN, 32, 16)
    p.overlay(face, p.grid(RUNNING_PERSON, W), 3, 3)
    p.outline(face, 14, 2, 20, 13, p.WHITE)  # door frame
    p.rect(face, 15, 3, 15, 12, p.WHITE)
    p.overlay(face, p.grid(ARROW_RIGHT, W), 22, 5)
    return _sign("schild_notausgang", "Schild Notausgang", face, wide=True)


def schild_sammelstelle() -> Asset:
    face = _plate(p.SAFETY_GREEN)
    for x in (4, 7, 10):  # group of people in the center
        p.overlay(face, p.grid(PERSON, W), x, 6)
    for (x, y), (dx, dy) in (((2, 2), (1, 1)), ((13, 2), (-1, 1)), ((2, 13), (1, -1)), ((13, 13), (-1, -1))):
        for step in range(3):  # arrows from the corners towards the center
            face.putpixel((x + dx * step, y + dy * step), p.shade(p.WHITE))
        face.putpixel((x + dx * 2, y + dy), p.shade(p.WHITE))
        face.putpixel((x + dx, y + dy * 2), p.shade(p.WHITE))
    return _sign("schild_sammelstelle", "Schild Sammelstelle", face)


def schild_erste_hilfe() -> Asset:
    face = _plate(p.SAFETY_GREEN)
    p.rect(face, 6, 3, 9, 12, p.WHITE)
    p.rect(face, 3, 6, 12, 9, p.WHITE)
    return _sign("schild_erste_hilfe", "Schild Erste Hilfe", face)


def schild_feuerloescher() -> Asset:
    face = _plate(p.RED)
    p.overlay(face, p.grid(EXTINGUISHER, W), 5, 3)
    p.overlay(face, p.grid(FLAME, W), 2, 3)
    return _sign("schild_feuerloescher", "Schild Feuerlöscher", face)


def schild_brandmelder() -> Asset:
    face = _plate(p.RED)
    p.overlay(face, p.grid(HAND, W), 2, 7)
    p.outline(face, 8, 4, 13, 9, p.WHITE)
    p.rect(face, 10, 6, 11, 7, p.WHITE)
    for x, y in ((8, 2), (11, 2), (14, 4)):  # alarm "waves"
        face.putpixel((x, y), p.shade(p.WHITE))
    return _sign("schild_brandmelder", "Schild Brandmelder", face)


def schild_loeschschlauch() -> Asset:
    face = _plate(p.RED)
    p.circle(face, 7, 7.5, 4.6, p.WHITE, ring=1.4)
    p.circle(face, 7, 7.5, 1.2, p.WHITE)
    p.rect(face, 10, 11, 13, 12, p.WHITE)  # hose leading to the nozzle
    p.rect(face, 13, 9, 13, 12, p.WHITE)
    return _sign("schild_loeschschlauch", "Schild Löschschlauch", face)


def fluchtplan() -> Asset:
    """Evacuation plan: floor plan with escape routes, exits, extinguishers and 'you are here'."""
    face = p.tile(p.WHITE, 32, 24)
    p.rect(face, 0, 0, 31, 2, p.SAFETY_GREEN)  # title band
    for x in range(3, 29, 3):
        face.putpixel((x, 1), p.shade(p.WHITE))
    p.outline(face, 2, 5, 29, 21, p.GREY)  # outer walls
    p.rect(face, 2, 12, 29, 12, p.GREY)  # corridor walls
    p.rect(face, 2, 15, 29, 15, p.GREY)
    for x in (10, 18, 24):  # room walls with door gaps
        p.rect(face, x, 5, x, 11, p.GREY)
        p.rect(face, x, 16, x, 21, p.GREY)
    for x in (6, 14, 21, 27):
        p.rect(face, x, 12, x + 1, 12, p.WHITE)
        p.rect(face, x, 15, x + 1, 15, p.WHITE)
    p.rect(face, 4, 13, 28, 14, p.rgb("#7fd39a"))  # escape route
    p.rect(face, 29, 13, 31, 14, p.SAFETY_GREEN)  # exit
    for x in (12, 20, 26):  # route arrows
        face.putpixel((x, 13), p.shade(p.SAFETY_GREEN))
        face.putpixel((x + 1, 14), p.shade(p.SAFETY_GREEN))
    for x, y in ((9, 7), (17, 19), (28, 7)):  # extinguishers
        p.rect(face, x, y, x, y + 1, p.RED)
    p.circle(face, 7, 18, 1.6, p.rgb("#d21e1e"))  # you are here
    face.putpixel((7, 18), p.shade(p.BLUE))
    atlas = Atlas()
    atlas.add("face", face)
    atlas.add("edge", p.tile(p.OFF_WHITE, 4, 4))
    model = Model("wall")
    model.box("board", (0, 2, 15.5), (16, 14, 16), "edge", north="face")
    return Asset("fluchtplan", "sign", "Flucht- und Rettungsplan", atlas, {"": model})


ALL = [schild_notausgang, schild_sammelstelle, schild_erste_hilfe, schild_feuerloescher, schild_brandmelder,
       schild_loeschschlauch, fluchtplan]
