#!/usr/bin/env python3
"""ORBITX brand asset generator.

Derives every launcher/splash/in-app brand raster from the single source
artwork (logo.jpg) so the mark can never drift between surfaces.

Outputs
-------
res/mipmap-<density>/ic_launcher.png            full-bleed squircle artwork
res/mipmap-<density>/ic_launcher_round.png      circular crop
res/mipmap-<density>/ic_launcher_foreground.png adaptive foreground (safe zone)
res/drawable-nodpi/orbitx_mark.png              ring only, alpha keyed, for dark UI
res/drawable-nodpi/orbitx_logo.png              full artwork, for splash/hero

Run from the module root:
    python3 tools/make_brand_assets.py <path-to-logo.jpg>
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

# Adaptive-icon geometry: 108dp canvas, 72dp safe zone (66.6%).
ADAPTIVE_SAFE = 0.66

LAUNCHER_SIZES = {
    "mdpi": 48, "hdpi": 72, "xhdpi": 96,
    "xxhdpi": 144, "xxxhdpi": 192,
}
FOREGROUND_SIZES = {
    "mdpi": 108, "hdpi": 162, "xhdpi": 216,
    "xxhdpi": 324, "xxxhdpi": 432,
}


def ring_bbox(im):
    """Bounding box of the saturated ring, ignoring the dark gradient field."""
    w, h = im.size
    px = im.load()
    minx, miny, maxx, maxy = w, h, -1, -1
    step = max(1, w // 540)
    for y in range(0, h, step):
        for x in range(0, w, step):
            r, g, b = px[x, y][:3]
            # Strong red only: the artwork's gradient field is far darker and
            # far less saturated than the ring band.
            if r > 110 and g < 72 and b < 72:
                if x < minx:
                    minx = x
                if x > maxx:
                    maxx = x
                if y < miny:
                    miny = y
                if y > maxy:
                    maxy = y
    if maxx < 0:
        raise SystemExit("could not locate the ring in the source artwork")
    return minx, miny, maxx, maxy


def square_crop(im, box, pad=0.04):
    """Expand `box` to a square with a little breathing room."""
    minx, miny, maxx, maxy = box
    cx, cy = (minx + maxx) / 2.0, (miny + maxy) / 2.0
    side = max(maxx - minx, maxy - miny) * (1.0 + pad * 2)
    half = side / 2.0
    w, h = im.size
    left = max(0, int(cx - half))
    top = max(0, int(cy - half))
    right = min(w, int(cx + half))
    bottom = min(h, int(cy + half))
    return im.crop((left, top, right, bottom))


def keyed_mark(im):
    """Ring on transparency, so it sits on any dark surface without a plate.

    Alpha comes from luminance (the ring is the only bright thing in frame),
    contrast-stretched so the field goes fully transparent and the ring keeps
    a soft edge instead of a hard matte. The stretch points are derived from
    the artwork's own histogram rather than hardcoded, because the source is
    a very dark plate (ring luminance peaks near 130, field near 20) and any
    fixed threshold either erases the ring or leaves the field fogged.
    """
    rgb = im.convert("RGB")
    lum = rgb.convert("L")

    hist = lum.histogram()
    total = float(sum(hist)) or 1.0

    def percentile(p, descending=False):
        acc = 0
        rng = range(255, -1, -1) if descending else range(256)
        for v in rng:
            acc += hist[v]
            if acc / total >= p:
                return v
        return 255 if descending else 0

    field = percentile(0.55)   # median-ish: the dark gradient plate
    band = percentile(0.02, descending=True)  # brightest 2%: ring core + glow
    lo = max(8, min(int(field) + 4, 60))
    hi = max(lo + 24, min(int(band), 255))
    alpha = lum.point(lambda v: 0 if v <= lo else min(255, int((v - lo) * 255 / (hi - lo))))
    alpha = alpha.filter(ImageFilter.GaussianBlur(0.6))
    out = rgb.copy()
    # Re-tint toward the brand ember so the mark reads identically on every
    # surface even where the artwork's own gradient cooled it down.
    out = Image.blend(out, Image.new("RGB", rgb.size, (255, 59, 48)), 0.25)
    out.putalpha(alpha)
    return out


def squircle(size, radius_ratio=0.235):
    """Anti-aliased superellipse-ish mask matching the source artwork plate."""
    ss = 4
    m = Image.new("L", (size * ss, size * ss), 0)
    d = ImageDraw.Draw(m)
    r = int(size * ss * radius_ratio)
    d.rounded_rectangle([0, 0, size * ss - 1, size * ss - 1], radius=r, fill=255)
    return m.resize((size, size), Image.LANCZOS)


def circle(size):
    ss = 4
    m = Image.new("L", (size * ss, size * ss), 0)
    ImageDraw.Draw(m).ellipse([0, 0, size * ss - 1, size * ss - 1], fill=255)
    return m.resize((size, size), Image.LANCZOS)


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: make_brand_assets.py <logo.jpg>")
    src = Image.open(sys.argv[1]).convert("RGB")
    res = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "res")

    artwork = square_crop(src, ring_bbox(src))
    mark = keyed_mark(artwork)

    written = []

    # ── launcher icon (full bleed) + round + adaptive foreground ──
    for density, size in LAUNCHER_SIZES.items():
        plate = artwork.resize((size, size), Image.LANCZOS).convert("RGBA")
        plate.putalpha(squircle(size))
        p = os.path.join(res, "mipmap-" + density, "ic_launcher.png")
        plate.save(p)
        written.append(p)

        round_plate = artwork.resize((size, size), Image.LANCZOS).convert("RGBA")
        round_plate.putalpha(circle(size))
        p = os.path.join(res, "mipmap-" + density, "ic_launcher_round.png")
        round_plate.save(p)
        written.append(p)

        fg_size = FOREGROUND_SIZES[density]
        inner = max(1, int(fg_size * ADAPTIVE_SAFE))
        canvas = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
        scaled = artwork.resize((inner, inner), Image.LANCZOS).convert("RGBA")
        scaled.putalpha(squircle(inner))
        off = (fg_size - inner) // 2
        canvas.paste(scaled, (off, off), scaled)
        p = os.path.join(res, "mipmap-" + density, "ic_launcher_foreground.png")
        canvas.save(p)
        written.append(p)

    # ── in-app marks ──
    mark.resize((512, 512), Image.LANCZOS).save(
        os.path.join(res, "drawable-nodpi", "orbitx_mark.png"))
    written.append(os.path.join(res, "drawable-nodpi", "orbitx_mark.png"))

    artwork.resize((720, 720), Image.LANCZOS).save(
        os.path.join(res, "drawable-nodpi", "orbitx_logo.png"))
    written.append(os.path.join(res, "drawable-nodpi", "orbitx_logo.png"))

    for p in written:
        print("wrote", os.path.normpath(p))


if __name__ == "__main__":
    main()
