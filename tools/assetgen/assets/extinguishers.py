"""Fire extinguishers (six agent types, each with draw and aim use frames) and the extinguishing spray.

Model states, switched by the client while right click is held (see content/items.yml, use_models):
    ""       idle: hose hangs down the side
    "_draw"  hose pulled out to the side
    "_aim"   hose and nozzle point along +x, which the "aim" display preset turns forward
"""

from __future__ import annotations

from dataclasses import dataclass

from .. import paint as p
from ..atlas import Atlas
from ..model import Model
from ..output import Asset


@dataclass(frozen=True)
class Kind:
    id: str
    title: str
    band: p.RGB  # label band color identifying the agent
    nozzle: str
    slim: bool = False


KINDS = (
    Kind("feuerloescher_abc", "ABC-Pulverlöscher", p.BLUE, "hose"),
    Kind("feuerloescher_co2", "CO2-Löscher", p.BLACK, "horn", slim=True),
    Kind("feuerloescher_schaum", "Schaumlöscher", p.rgb("#e8d8a0"), "foam"),
    Kind("feuerloescher_wasser", "Wasserlöscher", p.rgb("#3fb6d8"), "hose"),
    Kind("feuerloescher_fett", "Fettbrandlöscher", p.SAFETY_YELLOW, "lance"),
    Kind("feuerloescher_metall", "Metallbrandlöscher", p.rgb("#6a4fa0"), "shower"),
)

# Nozzle segments from the hose end outwards: (name, length, cross-section, material).
NOZZLES = {
    "hose": (("nozzle", 2.0, 1.5, "dark_metal"), ("nozzle_tip", 0.75, 1.0, "metal")),
    "horn": (("horn_grip", 1.5, 1.2, "dark_metal"), ("horn_1", 1.5, 1.6, "plastic"),
             ("horn_2", 1.5, 2.4, "plastic"), ("horn_3", 1.5, 3.2, "plastic")),
    "foam": (("foam_grip", 1.5, 1.5, "dark_metal"), ("foam_tube", 4.0, 1.6, "dark_metal"),
             ("foam_mouth", 0.5, 2.0, "metal")),
    "lance": (("lance_grip", 2.0, 1.5, "dark_metal"), ("lance_tube", 5.0, 0.6, "metal"),
              ("lance_tip", 0.5, 1.0, "metal")),
    "shower": (("shower_grip", 2.0, 1.5, "dark_metal"), ("shower_tube", 2.5, 0.7, "metal"),
               ("shower_head", 1.0, 3.0, "dark_metal")),
}

STATES = {"": "upright", "_draw": "upright", "_aim": "aim"}


def _body_side(band: p.RGB) -> "p.Image.Image":
    image = p.tile(p.RED, 12, 24)
    p.rect(image, 0, 0, 11, 1, p.RED_RIM)
    p.rect(image, 0, 22, 11, 23, p.RED_RIM)
    p.rect(image, 0, 8, 11, 15, p.WHITE)
    p.rect(image, 0, 11, 11, 12, band)
    for row in (9, 14):
        for x in range(2, 10):
            if x % 3 != 1:
                image.putpixel((x, row), p.shade(p.INK))
    return p.apply_profile(image, "cylinder")


def _gauge() -> "p.Image.Image":
    image = p.canvas(8, 8)
    p.circle(image, 3.5, 3.5, 3.9, p.METAL_DARK)
    p.circle(image, 3.5, 3.5, 2.9, p.WHITE)
    for x, y in ((5, 1), (6, 2), (6, 1)):
        image.putpixel((x, y), p.shade(p.rgb("#3cc85a")))
    for x, y in ((3, 4), (4, 3), (5, 2)):
        image.putpixel((x, y), p.shade(p.rgb("#d21e1e")))
    return image


def _top() -> "p.Image.Image":
    image = p.tile(p.RED, 12, 12, vertical=0.1)
    p.circle(image, 5.5, 5.5, 6.2, p.RED_DARK, ring=1.2)
    return image


def _atlas(band: p.RGB) -> Atlas:
    atlas = Atlas()
    atlas.add("body", _body_side(band))
    atlas.add("top", _top())
    atlas.add("shoulder", p.apply_profile(p.tile(p.RED_DARK, 12, 8), "cylinder"))
    atlas.add("metal", p.tile(p.METAL, 8, 8, vertical=0.35))
    atlas.add("dark_metal", p.tile(p.METAL_DARKER, 8, 8, vertical=0.3))
    atlas.add("hose", p.tile(p.BLACK, 8, 8, profile="cylinder", noise=0.05, seed=3))
    atlas.add("plastic", p.tile(p.PLASTIC, 8, 8, vertical=0.25))
    atlas.add("gauge", _gauge())
    return atlas


def _segment(model: Model, name: str, point: tuple[float, float, float], direction: str,
             length: float, cross: float, material: str) -> tuple[float, float, float]:
    x, y, z = point
    half = cross / 2
    if direction == "down":
        model.box(name, (x - half, y - length, z - half), (x + half, y, z + half), material)
        return x, y - length, z
    model.box(name, (x, y - half, z - half), (x + length, y + half, z + half), material)
    return x + length, y, z


def _nozzle(model: Model, kind: str, point: tuple[float, float, float], direction: str) -> None:
    for name, length, cross, material in NOZZLES[kind]:
        point = _segment(model, name, point, direction, length, cross, material)


def _nozzle_length(kind: str) -> float:
    return sum(segment[1] for segment in NOZZLES[kind])


def _extinguisher(kind: Kind, state: str) -> Model:
    model = Model(STATES[state])
    radius = 2.5 if kind.slim else 3.5
    height = 13 if kind.slim else 12
    c = 8.0
    # Three overlapping boxes approximate a cylinder.
    model.box("body", (c - radius + 0.5, 0, c - radius + 0.5), (c + radius - 0.5, height, c + radius - 0.5),
              "body", up="top", down="top")
    model.box("body_x", (c - radius, 0, c - radius + 1), (c + radius, height, c + radius - 1),
              "body", up="top", down="top")
    model.box("body_z", (c - radius + 1, 0, c - radius), (c + radius - 1, height, c + radius),
              "body", up="top", down="top")
    model.box("shoulder", (c - radius + 1, height, c - radius + 1), (c + radius - 1, height + 1, c + radius - 1),
              "shoulder", up="top", down="top")
    model.box("valve", (6.5, height + 1, 6.5), (9.5, height + 3, 9.5), "metal")
    model.box("carry_handle", (5.5, height + 3, 7.5), (11, height + 3.5, 8.5), "dark_metal")
    model.box("squeeze_lever", (6, height + 3.75, 7.5), (11.5, height + 4.25, 8.5), "metal",
              rotation=("z", 22.5, (7, height + 3.75, 8)))
    model.box("gauge", (5.25, height + 1.25, 7.25), (6.5, height + 2.75, 8.75), "dark_metal", west="gauge")

    side = c + radius  # outer x of the body
    outlet = height + 2  # hose center height at the valve
    model.box("hose_outlet", (9.5, outlet - 0.5, 7.5), (10.5, outlet + 0.5, 8.5), "hose")
    if state == "":
        bottom = max(1.0 + _nozzle_length(kind.nozzle), 4.0)
        model.box("hose_bend", (10.5, outlet - 0.5, 7.5), (side + 1, outlet + 0.5, 8.5), "hose")
        model.box("hose", (side, bottom, 7.5), (side + 1, outlet - 0.5, 8.5), "hose")
        _nozzle(model, kind.nozzle, (side + 0.5, bottom, 8), "down")
    elif state == "_draw":
        low = height - 3
        model.box("hose_bend", (10.5, outlet - 0.5, 7.5), (side + 1, outlet + 0.5, 8.5), "hose")
        model.box("hose", (side, low - 0.5, 7.5), (side + 1, outlet - 0.5, 8.5), "hose")
        model.box("hose_out", (side + 1, low - 0.5, 7.5), (side + 3, low + 0.5, 8.5), "hose")
        _nozzle(model, kind.nozzle, (side + 3, low, 8), "east")
    else:
        model.box("hose", (10.5, outlet - 0.5, 7.5), (side + 5, outlet + 0.5, 8.5), "hose")
        _nozzle(model, kind.nozzle, (side + 5, outlet, 8), "east")
    return model


def _extinguisher_asset(kind: Kind):
    def factory() -> Asset:
        return Asset(kind.id, "item", kind.title, _atlas(kind.band),
                     {state: _extinguisher(kind, state) for state in STATES})

    factory.__name__ = kind.id
    return factory


EXTINGUISHERS = [_extinguisher_asset(kind) for kind in KINDS]


def feuerloeschspray() -> Asset:
    """Aerosol fire extinguishing spray; the nozzle on the actuator points along +x (forward when aiming)."""
    atlas = Atlas()
    can = p.tile(p.RED, 8, 18)
    p.rect(can, 0, 0, 7, 0, p.RED_RIM)
    p.rect(can, 0, 5, 7, 12, p.WHITE)
    p.rect(can, 0, 8, 7, 9, p.ORANGE)
    atlas.add("can", p.apply_profile(can, "cylinder"))
    atlas.add("can_top", p.tile(p.METAL, 8, 8, vertical=0.2))
    atlas.add("cap", p.tile(p.PLASTIC, 8, 8, vertical=0.25))

    def build(preset: str) -> Model:
        model = Model(preset)
        model.box("can", (6, 0, 6.5), (10, 9, 9.5), "can", up="can_top", down="can_top")
        model.box("can_z", (6.5, 0, 6), (9.5, 9, 10), "can", up="can_top", down="can_top")
        model.box("shoulder", (6.5, 9, 6.5), (9.5, 10, 9.5), "can_top")
        model.box("stem", (7.5, 10, 7.5), (8.5, 10.5, 8.5), "can_top")
        model.box("actuator", (6.75, 10.5, 6.75), (9.25, 12.5, 9.25), "cap")
        model.box("nozzle", (9.25, 11.2, 7.6), (10.75, 11.9, 8.4), "cap")
        return model

    return Asset("feuerloeschspray", "item", "Feuerlöschspray", atlas, {"": build("upright"), "_aim": build("aim")})


ALL = [*EXTINGUISHERS, feuerloeschspray]
