# /// script
# requires-python = ">=3.11"
# dependencies = ["pillow>=11", "numpy>=2", "soundfile>=0.13"]
# ///
"""Everything is synthesized, nothing is downloaded. Usage: uv run tools/generate_assets.py"""

from pathlib import Path

from assetgen import hud, sound
from assetgen.assets import ALL_ASSETS
from assetgen.output import Output

ROOT = Path(__file__).resolve().parent.parent


def main() -> None:
    output = Output(ROOT / "content")
    for factory in ALL_ASSETS:
        asset = factory()
        output.asset(asset)
        print(f"  {asset.category:<9} {asset.id:<28} {len(asset.models)} model(s)")
    hud.write(output)
    sound.write(output)
    removed = output.finish(ROOT / "CREDITS.md")
    print(f"{len(output.written)} files written, CREDITS.md updated")
    for path in removed:
        print(f"  removed stale file content/{path}")


if __name__ == "__main__":
    main()
