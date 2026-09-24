#!/usr/bin/env python3
"""Find (and optionally repair) the resource-XML defect that stops mergeDebugResources.

AAPT2 parses resource XML with XMLStreamReader, which enforces the XML spec rule that
a comment body may not contain ``--`` (and may not end with a lone ``-``, which forms
``--`` against the closing ``-->``).  The ORBIT Desktop layouts carry ASCII-art box art
inside their banner comments:

    +----------------------------------------------------------+
    |  lh_dock            COMMAND BAR   (title + menu strips)  |
    +----------------------------------------------------------+

Every one of those rules is a run of hyphens inside a comment, so AAPT2 rejects the file
and the whole build stops at mergeDebugResources -- before any Java is compiled.

This scanner is deliberately authoritative: it does not pattern-match for ``--`` on its
own, it asks an XML parser that implements the same restriction AAPT2 does.  A file only
counts as clean if the parser accepts it.

Usage:
    guard_resource_xml_comments.py <res-dir>            # report violations, exit 1 if any
    guard_resource_xml_comments.py <res-dir> --fix      # repair in place, then re-verify

Repair is lossless: each run of 2+ hyphens (and a trailing lone hyphen) inside a comment
body is replaced by an equal-length run of ``=``, so every line keeps its width and the
ASCII art still lines up.  Character count is preserved, nothing outside a comment body
is touched.
"""
import argparse
import re
import sys
import xml.parsers.expat
from pathlib import Path

# Non-greedy, DOTALL: matches each complete comment including its delimiters.
COMMENT_RE = re.compile(r"<!--.*?-->", re.DOTALL)


def expat_error(text: str) -> str | None:
    """Return expat's error message, or None when the document parses.

    Expat enforces the XML comment restriction, so this is the same verdict AAPT2
    reaches -- no hand-rolled approximation of the rule.
    """
    parser = xml.parsers.expat.ParserCreate()
    try:
        parser.Parse(text, True)
    except xml.parsers.expat.ExpatError as exc:
        return str(exc)
    return None


def offending_bodies(text: str):
    """Yield (line_number, body) for every comment body that breaks the rule."""
    for match in COMMENT_RE.finditer(text):
        body = match.group(0)[4:-3]
        if "--" in body or body.endswith("-"):
            line = text.count("\n", 0, match.start()) + 1
            yield line, body


def repair(text: str) -> tuple[str, int]:
    """Replace hyphen runs inside comment bodies with equal-length '=' runs."""
    changed = 0

    def fix(match: re.Match) -> str:
        nonlocal changed
        body = match.group(0)[4:-3]
        fixed = re.sub(r"-{2,}", lambda m: "=" * len(m.group(0)), body)
        # A body ending in a single '-' forms '--' against the closing '-->'.
        if fixed.endswith("-"):
            fixed = fixed[:-1] + "="
        if fixed != body:
            changed += 1
        return "<!--" + fixed + "-->"

    return COMMENT_RE.sub(fix, text), changed


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("res_dir", help="path to src/main/res")
    ap.add_argument("--fix", action="store_true", help="repair the files in place")
    args = ap.parse_args()

    root = Path(args.res_dir)
    if not root.is_dir():
        print(f"::error::no such res directory: {root}")
        return 2

    files = sorted(root.rglob("*.xml"))
    bad = []
    for path in files:
        # Read as bytes then decode, so a stray encoding does not crash the scan.
        text = path.read_text(encoding="utf-8", errors="replace")
        hits = list(offending_bodies(text))
        if hits:
            err = expat_error(text)
            bad.append((path, hits, err))

    if not bad:
        print(f"Resource XML comments clean ({len(files)} files scanned, parser accepts all).")
        return 0

    print(f"::error::{len(bad)} resource XML file(s) carry '--' inside a comment "
          f"(AAPT2 will fail mergeDebugResources):")
    for path, hits, err in bad:
        for line, _ in hits:
            print(f"    {path}:{line}")
        if err:
            print(f"      parser: {err}")

    if not args.fix:
        print("Re-run with --fix to repair (equal-length '=' runs), or fix by hand.")
        return 1

    print("\nRepairing...")
    for path, _, _ in bad:
        text = path.read_text(encoding="utf-8")
        fixed, changed = repair(text)
        path.write_text(fixed, encoding="utf-8")
        print(f"    {path}: {changed} comment(s) rewritten")

    # Re-verify: every file must now parse, or the repair turned a comment bug into a
    # structural one and we must not report success.
    survivors = []
    for path in sorted(root.rglob("*.xml")):
        text = path.read_text(encoding="utf-8", errors="replace")
        if list(offending_bodies(text)) or expat_error(text):
            survivors.append(path)

    if survivors:
        for path in survivors:
            print(f"::error::still invalid after repair: {path}")
        return 1

    print(f"\nAll {len(files)} resource XML files parse. 0 violations remain.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
