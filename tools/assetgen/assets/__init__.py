"""Asset definitions, grouped by topic. ALL_ASSETS lists every asset factory in output order."""

from . import building, electrical, equipment, extinguishers, kitchen, signs

ALL_ASSETS = [
    *extinguishers.ALL,
    *equipment.ALL,
    *electrical.ALL,
    *building.ALL,
    *kitchen.ALL,
    *signs.ALL,
]
