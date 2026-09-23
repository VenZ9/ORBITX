#!/usr/bin/env python3
"""Strip the CS-client ('modded_profile_bta') card elements from the profile-type
layouts. Removes the whole XML element, including nested children, by matching
the opening tag that precedes the target id and consuming until depth balances.

Run from the repo root:  python3 tools/strip_cs_cards.py
"""
import re
import sys

TARGET_ID = 'android:id="@+id/modded_profile_bta"'
FILES = [
    "android/orbitx_launcher/src/main/res/layout/fragment_profile_type.xml",
    "android/orbitx_launcher/src/main/res/layout-land/fragment_profile_type.xml",
]


def strip_element(text, tag, start_of_id):
    """Remove the <tag ...>...</tag> element containing start_of_id."""
    open_pat = re.compile(r"<" + tag + r"\b")
    # walk backwards to the nearest opening tag of this name before the id
    open_start = None
    for m in open_pat.finditer(text, 0, start_of_id):
        open_start = m.start()
    if open_start is None:
        return None
    # walk forwards balancing the same tag name
    depth = 0
    pos = open_start
    close_re = re.compile(r"</?" + tag + r"\b[^>]*>")
    while True:
        m = close_re.search(text, pos)
        if not m:
            return None
        token = m.group(0)
        if token.startswith("</"):
            depth -= 1
            if depth == 0:
                end = m.end()
                # swallow the trailing newline + indentation of the closed line
                nl = text.find("\n", end)
                if nl != -1:
                    end = nl + 1
                # also swallow leading whitespace on the opening line
                line_start = text.rfind("\n", 0, open_start)
                begin = line_start + 1 if line_start != -1 else open_start
                if text[begin:open_start].strip() != "":
                    begin = open_start
                return text[:begin] + text[end:]
        else:
            if token.endswith("/>"):
                pass  # self-closing, no depth change
            else:
                depth += 1
        pos = m.end()


def main():
    changed = 0
    for path in FILES:
        try:
            with open(path, "r", encoding="utf-8") as fh:
                text = fh.read()
        except FileNotFoundError:
            print("MISSING " + path)
            continue
        removed = 0
        while True:
            idx = text.find(TARGET_ID)
            if idx == -1:
                break
            out = strip_element(text, "FrameLayout", idx)
            if out is None:
                print("COULD NOT MATCH in " + path)
                sys.exit(1)
            text = out
            removed += 1
        # any surviving CS promo background falls back to the standard card bg
        subs = text.count("@drawable/bg_cs_client_promo_card")
        text = text.replace("@drawable/bg_cs_client_promo_card", "@drawable/bg_profile_type_card")
        # the CS logo tile died with the card; anything left over uses the app mark
        logo = text.count("@drawable/cs_logo")
        text = text.replace("@drawable/cs_logo", "@drawable/orbitx_mark")
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)
        print("%s: removed %d card(s), retargeted %d bg, %d logo" % (path, removed, subs, logo))
        changed += removed
    print("total cards removed: %d" % changed)


if __name__ == "__main__":
    main()
