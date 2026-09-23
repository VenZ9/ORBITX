#!/usr/bin/env python3
"""One-shot: re-map the violet/indigo/blue accent family onto the ORBITX ember
palette across an Android res tree.

Preserves each token's saturation and lightness (via HLS round-trip) so silver
and graphite elements stay silver/graphite and only the *hue* moves. Also gives
near-black surfaces a warm red undertone instead of a blue one.
"""
import colorsys
import os
import re
import sys

HEX = re.compile(r'#([0-9A-Fa-f]{8}|[0-9A-Fa-f]{6})')

# source hue band -> destination hue
BANDS = [
    (195, 225, 356),   # cyan / azure      -> ember
    (225, 255, 0),     # blue              -> ember
    (255, 285, 355),   # indigo / violet   -> ember red
    (285, 315, 350),   # purple            -> crimson
    (315, 346, 347),   # magenta / pink    -> deep red
]
MIN_SAT = 0.08
MAX_SAT = 0.86


def target_hue(h):
    for lo, hi, dst in BANDS:
        if lo <= h < hi:
            return dst
    return None


def shift(token):
    body = token[1:]
    alpha = body[:-6] if len(body) == 8 else ""
    s6 = body[-6:]
    r, g, b = (int(s6[i:i + 2], 16) / 255.0 for i in (0, 2, 4))
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    hue = h * 360.0
    dst = target_hue(hue)
    if dst is None or s < MIN_SAT:
        return token
    s = min(s, MAX_SAT)
    nr, ng, nb = colorsys.hls_to_rgb(dst / 360.0, l, s)
    out = "%02X%02X%02X" % (round(nr * 255), round(ng * 255), round(nb * 255))
    return "#" + alpha + out


def main():
    roots = sys.argv[1:] or ["."]
    changed_files = 0
    changed_tokens = 0
    for root in roots:
        for base, _, files in os.walk(root):
            for f in files:
                if not f.endswith(".xml"):
                    continue
                p = os.path.join(base, f)
                try:
                    src = open(p, encoding="utf-8").read()
                except (OSError, UnicodeDecodeError):
                    continue
                out, n = HEX.subn(lambda m: shift(m.group(0)), src)
                if n and out != src:
                    # count real changes
                    real = sum(1 for a, b in zip(HEX.findall(src), HEX.findall(out)) if a != b)
                    if real:
                        open(p, "w", encoding="utf-8").write(out)
                        changed_files += 1
                        changed_tokens += real
    print("files rewritten:", changed_files)
    print("tokens recoloured:", changed_tokens)


if __name__ == "__main__":
    main()
