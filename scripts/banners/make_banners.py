"""Build the description banners for the CurseForge / Modrinth pages.

Header: the given in-game screenshots cut into slanted panels, with the mod title on top.
Section dividers: a dark strip with a pixel-font heading for every description section.

Only real screenshots and a free font (Press Start 2P, SIL OFL, see fonts/OFL.txt) are used,
so the output contains no generated imagery.

Usage:
    python scripts/banners/make_banners.py --shots a.png b.png c.png [--out docs/images]
Set LOG_LEVEL=DEBUG for per-step details.
"""
import argparse
import logging
import os
import sys
import time
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

LOG = logging.getLogger("banners")

HERE = Path(__file__).resolve().parent
FONT_PATH = HERE / "fonts" / "PressStart2P-Regular.ttf"

HEADER_SIZE = (1920, 720)
DIVIDER_SIZE = (1920, 200)

CREAM = (243, 229, 192)
GOLD = (201, 162, 39)
INK = (20, 14, 26)
BG_LEFT = (36, 23, 48)
BG_RIGHT = (18, 24, 38)

SECTIONS = {
    "en": [
        ("getting_started", "GETTING STARTED"),
        ("features", "FEATURES"),
        ("compatibility", "COMPATIBILITY"),
        ("configuration", "CONFIGURATION"),
        ("photosensitivity", "PHOTOSENSITIVITY WARNING"),
        ("notes", "NOTES"),
    ],
    "ru": [
        ("getting_started", "С ЧЕГО НАЧАТЬ"),
        ("features", "ВОЗМОЖНОСТИ"),
        ("compatibility", "СОВМЕСТИМОСТЬ"),
        ("configuration", "НАСТРОЙКИ"),
        ("photosensitivity", "ПРЕДУПРЕЖДЕНИЕ О ВСПЫШКАХ"),
        ("notes", "ПРИМЕЧАНИЯ"),
    ],
}


def setup_logging():
    # The Windows console defaults to a legacy code page, which turns Cyrillic section names into '?'.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
    level = os.environ.get("LOG_LEVEL", "INFO").upper()
    logging.basicConfig(
        level=getattr(logging, level, logging.INFO),
        format="%(asctime)s.%(msecs)03d %(levelname)s %(name)s: %(message)s",
        datefmt="%H:%M:%S",
        stream=sys.stdout,
    )

    def excepthook(exc_type, exc, tb):
        LOG.critical("banner build crashed", exc_info=(exc_type, exc, tb))

    sys.excepthook = excepthook


def font(size):
    LOG.debug("loading font %s at %dpx", FONT_PATH.name, size)
    return ImageFont.truetype(str(FONT_PATH), size)


def cover(img, box_w, box_h, fx=0.5):
    """Scale img to fully cover box_w x box_h and crop it; fx picks the horizontal crop (0 left, 1 right)."""
    scale = max(box_w / img.width, box_h / img.height)
    resized = img.resize((round(img.width * scale), round(img.height * scale)), Image.LANCZOS)
    left = round((resized.width - box_w) * fx)
    top = (resized.height - box_h) // 2
    LOG.debug("cover %dx%d -> %dx%d (scale %.3f, focus %.2f, crop at %d,%d)",
              img.width, img.height, box_w, box_h, scale, fx, left, top)
    return resized.crop((left, top, left + box_w, top + box_h))


def pixel_text(draw, xy, text, fnt, fill, shadow, step):
    """Hard-edged text with a dark outline and a one-pixel-unit drop shadow."""
    x, y = xy
    draw.fontmode = "1"
    draw.text((x + step, y + step), text, font=fnt, fill=shadow, stroke_width=step // 2, stroke_fill=shadow)
    draw.text((x, y), text, font=fnt, fill=fill, stroke_width=step // 2, stroke_fill=shadow)


def text_size(fnt, text):
    left, top, right, bottom = fnt.getbbox(text)
    return right - left, bottom - top


def parse_shot(spec):
    """'path' or 'path@0.4' -> (path, horizontal focus 0..1)."""
    path, sep, focus = spec.rpartition("@")
    if not sep:
        return spec, 0.5
    try:
        fx = float(focus)
    except ValueError:
        LOG.warning("ignoring focus %r in %r: not a number, using the centre", focus, spec)
        return path, 0.5
    if not 0.0 <= fx <= 1.0:
        LOG.warning("focus %.2f in %r is outside 0..1, clamping", fx, spec)
        fx = min(1.0, max(0.0, fx))
    return path, fx


def build_header(shots, title, subtitle, title_pos, out_path):
    started = time.perf_counter()
    w, h = HEADER_SIZE
    n = len(shots)
    LOG.info("header: %d screenshot panel(s), title=%r at %s, subtitle=%r", n, title, title_pos, subtitle)
    canvas = Image.new("RGB", (w, h), INK)
    slant = 160 if n > 1 else 0
    panel_w = w / n
    for i, (path, fx) in enumerate(shots):
        src = Image.open(path).convert("RGB")
        LOG.info("panel %d/%d: %s (%dx%d, focus %.2f)", i + 1, n, path, src.width, src.height, fx)
        x0 = round(i * panel_w - slant / 2)
        x1 = round((i + 1) * panel_w + slant / 2)
        box_left = max(0, x0)
        box_right = min(w, x1)
        tile = cover(src, box_right - box_left, h, fx)
        mask = Image.new("L", (w, h), 0)
        top_left = 0 if i == 0 else x0 + slant
        top_right = w if i == n - 1 else x1
        bottom_left = 0 if i == 0 else x0
        bottom_right = w if i == n - 1 else x1 - slant
        ImageDraw.Draw(mask).polygon(
            [(top_left, 0), (top_right, 0), (bottom_right, h), (bottom_left, h)], fill=255)
        layer = Image.new("RGB", (w, h))
        layer.paste(tile, (box_left, 0))
        canvas.paste(layer, (0, 0), mask)
    draw = ImageDraw.Draw(canvas)
    for i in range(1, n):
        x = round(i * panel_w)
        draw.line([(x + slant / 2, 0), (x - slant / 2, h)], fill=INK, width=12)
    LOG.debug("panels composed, adding the title band")

    band = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    band_draw = ImageDraw.Draw(band)
    # "top" keeps the title over the sky so the hookahs in the lower half stay visible.
    band_top, band_bottom = (round(h * 0.02), round(h * 0.42)) if title_pos == "top" else (round(h * 0.30), round(h * 0.74))
    for y in range(band_top, band_bottom):
        edge = min(y - band_top, band_bottom - y) / ((band_bottom - band_top) / 2)
        band_draw.line([(0, y), (w, y)], fill=(*INK, round(190 * min(1.0, edge * 2.2))))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), band)
    draw = ImageDraw.Draw(canvas)

    title_font = font(128)
    tw, th = text_size(title_font, title)
    tx, ty = (w - tw) // 2, band_top + (40 if title_pos == "top" else 70)
    pixel_text(draw, (tx, ty), title, title_font, CREAM, INK, 16)
    if subtitle:
        sub_font = font(32)
        sw, _ = text_size(sub_font, subtitle)
        pixel_text(draw, ((w - sw) // 2, ty + th + 36), subtitle, sub_font, GOLD, INK, 4)
    canvas.convert("RGB").save(out_path, optimize=True)
    LOG.info("header saved: %s (%dx%d, %d bytes) in %d ms",
             out_path, w, h, out_path.stat().st_size, (time.perf_counter() - started) * 1000)


def build_divider(label, out_path):
    started = time.perf_counter()
    w, h = DIVIDER_SIZE
    canvas = Image.new("RGB", (w, h))
    draw = ImageDraw.Draw(canvas)
    for x in range(w):
        t = x / (w - 1)
        draw.line([(x, 0), (x, h)], fill=tuple(round(a + (b - a) * t) for a, b in zip(BG_LEFT, BG_RIGHT)))
    label_font = font(48)
    lw, lh = text_size(label_font, label)
    lx, ly = 96, (h - lh) // 2 - 8
    pixel_text(draw, (lx, ly), label, label_font, CREAM, INK, 6)
    bar_y = ly + lh + 28
    draw.rectangle([lx, bar_y, lx + lw + 48, bar_y + 6], fill=GOLD)
    draw.rectangle([lx + lw + 64, bar_y, lx + lw + 70, bar_y + 6], fill=GOLD)
    canvas.save(out_path, optimize=True)
    LOG.info("divider saved: %s (%r, %d bytes) in %d ms",
             out_path.name, label, out_path.stat().st_size, (time.perf_counter() - started) * 1000)


def main():
    setup_logging()
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--shots", nargs="+", required=True,
                        help="1-4 in-game screenshots for the header, left to right; "
                             "append @0..1 to pick the horizontal crop, e.g. shot.png@0.3")
    parser.add_argument("--out", default="docs/images", help="output directory")
    parser.add_argument("--title", default="HOOKAHS")
    parser.add_argument("--subtitle", default="SMOKE WITH YOUR FRIENDS")
    parser.add_argument("--title-pos", choices=["top", "center"], default="top")
    args = parser.parse_args()

    started = time.perf_counter()
    LOG.info("building banners: %d screenshot(s), out=%s, font=%s", len(args.shots), args.out, FONT_PATH)
    if not FONT_PATH.is_file():
        LOG.error("font not found at %s - download Press Start 2P (OFL) into scripts/banners/fonts/", FONT_PATH)
        return 2
    if not 1 <= len(args.shots) <= 4:
        LOG.error("expected 1-4 screenshots for the header, got %d", len(args.shots))
        return 2
    shots = [parse_shot(s) for s in args.shots]
    missing = [p for p, _ in shots if not Path(p).is_file()]
    if missing:
        LOG.error("screenshot(s) not found: %s", missing)
        return 2

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    build_header(shots, args.title, args.subtitle, args.title_pos, out / "header.png")
    count = 1
    for lang, sections in SECTIONS.items():
        for key, label in sections:
            build_divider(label, out / f"section_{lang}_{key}.png")
            count += 1
    LOG.info("done: %d image(s) in %s in %d ms", count, out, (time.perf_counter() - started) * 1000)
    return 0


if __name__ == "__main__":
    sys.exit(main())
