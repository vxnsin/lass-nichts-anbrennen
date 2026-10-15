"""Packs named tiles into a square texture atlas and maps tile names to model UVs."""

from __future__ import annotations

from PIL import Image

MAX_SIZE = 256


class Atlas:
    """Collects tiles for one model texture. UVs are expressed in model space (0-16)."""

    def __init__(self) -> None:
        self._tiles: dict[str, Image.Image] = {}

    def add(self, name: str, image: Image.Image) -> str:
        if name in self._tiles:
            raise ValueError(f"tile '{name}' already exists")
        self._tiles[name] = image
        return name

    def __contains__(self, name: str) -> bool:
        return name in self._tiles

    def build(self) -> tuple[Image.Image, dict[str, list[float]]]:
        # Deterministic order: tallest first, then widest, then by name (shelf packing).
        order = sorted(self._tiles.items(), key=lambda item: (-item[1].height, -item[1].width, item[0]))
        size = 16
        while (placements := self._pack(order, size)) is None:
            size *= 2
            if size > MAX_SIZE:
                raise ValueError(f"tiles do not fit into a {MAX_SIZE}x{MAX_SIZE} atlas")

        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        scale = 16 / size
        uvs: dict[str, list[float]] = {}
        for name, (x, y) in placements.items():
            tile = self._tiles[name]
            image.paste(tile, (x, y))
            uvs[name] = [round(x * scale, 4), round(y * scale, 4),
                         round((x + tile.width) * scale, 4), round((y + tile.height) * scale, 4)]
        return image, uvs

    @staticmethod
    def _pack(order: list[tuple[str, Image.Image]], size: int) -> dict[str, tuple[int, int]] | None:
        placements: dict[str, tuple[int, int]] = {}
        x = y = row_height = 0
        for name, tile in order:
            if tile.width > size:
                return None
            if x + tile.width > size:
                x, y, row_height = 0, y + row_height, 0
            if y + tile.height > size:
                return None
            placements[name] = (x, y)
            x += tile.width
            row_height = max(row_height, tile.height)
        return placements
