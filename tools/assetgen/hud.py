"""HUD glyph textures (rendered via the lna:hud font, see content/glyphs.yml)."""

from __future__ import annotations

from . import paint as p
from .output import Output

PALETTE = {
    "O": p.rgb("#4a0710"),
    "R": p.rgb("#c8102e"),
    "H": p.rgb("#e8454f"),
    "S": p.rgb("#8e0b1f"),
    "M": p.rgb("#c4c8cc"),
    "m": p.rgb("#6e7378"),
    "K": p.rgb("#1e1e20"),
    "k": p.rgb("#464646"),
    "W": p.rgb("#f2f2f2"),
    "B": p.rgb("#2b6cd8"),
    "e": (60, 60, 60, 200),
    "f": (95, 95, 95, 200),
}

EXTINGUISHER_ICON = [
    "..mmmm..",
    "..KmM...",
    ".K.OO...",
    ".KOHRO..",
    ".KOWWO..",
    ".KOBBO..",
    ".kOHRO..",
    "..OOOO..",
]
BAR_FULL = ["HHH", "RRR", "RRR", "RRR", "RRR", "SSS", "SSS"]
BAR_EMPTY = ["fff", "eee", "eee", "eee", "eee", "eee", "eee"]


def write(output: Output) -> None:
    output.image("textures/hud/feuerloescher.png", p.grid(EXTINGUISHER_ICON, PALETTE), "Feuerlöscher-Symbol (HUD)")
    output.image("textures/hud/bar_full.png", p.grid(BAR_FULL, PALETTE), "Füllstandsbalken voll (HUD)")
    output.image("textures/hud/bar_empty.png", p.grid(BAR_EMPTY, PALETTE), "Füllstandsbalken leer (HUD)")
