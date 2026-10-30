"""Generate TagSpace marker images (bundled in the app) and a printable A4 PDF per marker.

Usage: python tools/generate_markers.py [count]

Each marker is a seeded random collage of shapes: unique, non-repeating and full of corners,
which is what ARCore Augmented Images needs to detect it reliably.
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "markers"
PRINTS = ROOT / "markers" / "out"

MARKER_PX = 1000
MARKER_WIDTH_CM = 15.0  # must match Marker.physicalWidthM in the app
DPI = 300
PX_PER_CM = DPI / 2.54


def marker_image(seed: int) -> Image.Image:
    rng = random.Random(seed)
    img = Image.new("L", (MARKER_PX, MARKER_PX), 255)
    d = ImageDraw.Draw(img)
    for size in (260, 140, 70, 35):  # coarse to fine so features exist at every viewing distance
        for _ in range(int(9000 / size)):
            x, y = rng.randrange(MARKER_PX), rng.randrange(MARKER_PX)
            s = rng.randrange(size // 2, size)
            shade = rng.choice((0, 0, 40, 90, 160, 220, 255))
            kind = rng.randrange(3)
            if kind == 0:
                d.rectangle((x, y, x + s, y + s * rng.uniform(0.3, 1.2)), fill=shade)
            elif kind == 1:
                d.ellipse((x, y, x + s, y + s), fill=shade)
            else:
                pts = [(x + rng.randrange(-s, s), y + rng.randrange(-s, s)) for _ in range(3)]
                d.polygon(pts, fill=shade)
    d.rectangle((0, 0, MARKER_PX - 1, MARKER_PX - 1), outline=0, width=24)
    return img


def print_page(marker: Image.Image, marker_id: str) -> Image.Image:
    page = Image.new("L", (round(21.0 * PX_PER_CM), round(29.7 * PX_PER_CM)), 255)
    side = round(MARKER_WIDTH_CM * PX_PER_CM)
    left = (page.width - side) // 2
    top = round(3 * PX_PER_CM)
    page.paste(marker.resize((side, side), Image.NEAREST), (left, top))

    d = ImageDraw.Draw(page)
    try:
        font = ImageFont.truetype("arial.ttf", 60)
        small = ImageFont.truetype("arial.ttf", 40)
    except OSError:
        font = small = ImageFont.load_default()
    y = top + side + round(1 * PX_PER_CM)
    d.text((left, y), f"TagSpace  {marker_id}  -  My room", fill=0, font=font)

    # 10 cm scale bar with 1 cm ticks: measure it with a ruler to verify the print scale.
    y += round(2 * PX_PER_CM)
    bar = round(10 * PX_PER_CM)
    d.rectangle((left, y, left + bar, y + 12), fill=0)
    for cm in range(11):
        x = left + round(cm * PX_PER_CM)
        d.rectangle((x - 3, y - 30, x + 3, y + 12), fill=0)
    d.text((left, y + 40), "This bar must measure exactly 10 cm. Print at 100% / actual size.", fill=0, font=small)
    d.text((left, y + 100), f"Marker square must measure {MARKER_WIDTH_CM:.0f} cm.", fill=0, font=small)
    return page


def main() -> None:
    count = int(sys.argv[1]) if len(sys.argv) > 1 else 2
    ASSETS.mkdir(parents=True, exist_ok=True)
    PRINTS.mkdir(parents=True, exist_ok=True)
    for i in range(1, count + 1):
        marker_id = f"marker-{i:02d}"
        img = marker_image(seed=7919 * i)
        img.save(ASSETS / f"{marker_id}.png")
        print_page(img, marker_id).save(PRINTS / f"{marker_id}.pdf", resolution=DPI)
        print(f"{marker_id}: {ASSETS / (marker_id + '.png')}  {PRINTS / (marker_id + '.pdf')}")


if __name__ == "__main__":
    main()
