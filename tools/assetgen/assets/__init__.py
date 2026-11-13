"""ALL_ASSETS lists every asset factory in output order."""

from . import building, decor, electrical, equipment, extinguishers, kitchen, signs

ALL_ASSETS = [
    *extinguishers.ALL,
    *equipment.ALL,
    *electrical.ALL,
    *building.ALL,
    *kitchen.ALL,
    *decor.ALL,
    *signs.ALL,
]
