#!/usr/bin/env python3
"""
ORBIT Desktop migration — sweep the remaining phone-shaped layouts onto the
ORBIT Desktop language, then gate the result.

    python3 tools/od_migrate.py --apply    run the passes, then the gates
    python3 tools/od_migrate.py --gate     gates only, change nothing
    python3 tools/od_migrate.py --list     report the target set

Exit status is 0 only if every gate passes, so a CI step that commits the result
can refuse to commit when a gate fails.

FOUR PHONE IDIOMS -> THE od_ LANGUAGE

  1. @dimen/_NNsdp (Intuit sdp, density-scaled to the screen width) -> a fixed dp
     on the od_ grid or an od_ furniture constant. Density scaling exists to make
     one design fit every phone width; a window has one width and fixed chrome,
     and scaling a command bar to the screen is exactly what stops it reading as
     chrome.
  2. @dimen/_NNssp -> the od_ six-step type scale.
  3. android:textColor="#RRGGBB" -> one of five od_ text roles. The hardcoded set
     is the ad-hoc palette of four earlier rounds; naming the roles collapses it.
  4. rounded / glass card backgrounds -> od_ planes and rows. A card is a box with
     a radius; a pane is a plane. The box is the idiom being removed.

WHAT IT NEVER CHANGES

  No android:id. Not renamed, not removed, not retyped — and the gates prove it
  rather than asserting it.

GATES (see the two functions at the bottom)

  gate 1  per file  : parses; loses no id; every reference it INTRODUCES resolves
  gate 2  whole tree: no id changes the element type Java casts it to; every
                      resource reference in every layout resolves

A NOTE ON WHAT "PARSES" MEANS HERE

  It means AAPT2 accepts it, and AAPT2 is stricter than it looks: a double hyphen
  inside an XML comment is a hard failure of mergeDebugResources, not a warning.
  gate 1 therefore parses each file AS IS, comments included, and it checks every
  XML resource under res/ rather than only the layouts — a values/ file that will
  not parse stops the build just as hard as a broken layout does. An earlier
  version of this gate stripped comments first and so reported PASS on a tree
  whose od_design.xml could not be merged; do not reintroduce that shortcut.
"""
import os
import re
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.environ.get("OD_RES") or os.path.normpath(
    os.path.join(HERE, "..", "android", "orbitx_launcher", "src", "main", "res"))
REPO = os.environ.get("OD_REPO") or os.path.normpath(os.path.join(HERE, ".."))
LAYOUT = os.path.join(RES, "layout")

# -- dp: the phone scale -> the od_ grid / od_ furniture ----------------------
#  Chosen so the rendered size moves at most 2dp, which is why the sweep cannot
#  reflow a layout. Values above 60 stay literal so no hero dimension collapses.
DP = {
    0: "od_space_0", 1: "od_space_1", 2: "od_radius_sharp", 3: "od_space_1",
    4: "od_space_1", 5: "od_space_1", 6: "od_space_1", 7: "od_space_2",
    8: "od_space_2", 9: "od_space_2", 10: "od_space_2", 11: "od_space_3",
    12: "od_space_3", 13: "od_space_3", 14: "od_gutter", 15: "od_space_4",
    16: "od_space_4", 17: "od_space_4", 18: "od_space_4", 19: "od_space_5",
    20: "od_space_5", 21: "od_space_5", 22: "od_space_6", 23: "od_space_6",
    24: "od_space_6", 25: "od_space_6", 26: "od_status_h", 27: "od_status_h",
    28: "od_thumb", 29: "od_status_h", 30: "od_commandbar_brand",
    31: "od_commandbar_brand", 32: "od_space_8", 33: "od_space_8",
    34: "od_well_w", 35: "od_well_w", 36: "od_thumb_lg", 37: "od_thumb_lg",
    38: "od_space_10", 39: "od_space_10", 40: "od_space_10",
    41: "od_space_10", 42: "od_space_10", 43: "od_row_h_toolbar",
    44: "od_row_h_toolbar", 45: "od_row_h_toolbar", 46: "od_pane_header_h",
    47: "od_pane_header_h", 48: "od_row_h", 49: "od_row_h", 50: "od_row_h",
    51: "od_row_h", 52: "od_row_h", 53: "od_row_h", 54: "od_row_h_tall",
    55: "od_row_h_tall", 56: "od_row_h_tall", 57: "od_row_h_tall",
    58: "od_row_h_tall", 59: "od_row_h_tall", 60: "od_row_h_tall",
}
BIG = {65: "65dp"}

# -- sp: the ad-hoc scale -> the od_ six --------------------------------------
SP = {"5": "od_text_label", "6": "od_text_label", "6.5": "od_text_label",
      "6.8": "od_text_label", "7": "od_text_label", "7.5": "od_text_label",
      "8": "od_text_label", "8.5": "od_text_label", "9": "od_text_label",
      "9.5": "od_text_label", "10": "od_text_label", "10.5": "od_text_label",
      "11": "od_text_mono", "11.5": "od_text_body_s", "12": "od_text_body_s",
      "12.5": "od_text_body", "13": "od_text_title_s", "13.5": "od_text_title_s",
      "14": "od_text_title_s", "14.5": "od_text_title_m", "15": "od_text_title_m",
      "15.5": "od_text_title_m", "16": "od_text_title_m",
      "16.5": "od_text_title_m", "17": "od_text_title_m",
      "18": "od_text_title_l", "19": "od_text_title_l", "20": "od_text_title_l",
      "21": "od_text_title_l", "22": "od_text_title_l"}

# -- text colours: the ad-hoc palette -> the five text roles ------------------
#   near-white emphasis -> od_text       mid grey       -> od_text_dim
#   caption / meta      -> od_text_muted footnote/hint -> od_text_faint
#   the ember family    -> od_accent_bright
#   the semantic live/warn/error values keep their meaning
#   dark values read as ink ON an accent fill -> od_on_accent
COLOR = {}
for _h in ("ffffff f9f4f4 f3f0f0 f4eeee fcf8f8 f5f2f2 f4f2f2 f0f0f2 f5f0f0 "
           "f2eded efe6e7 f0e2e3 ded2d3 e5dadb fafafa f9f1f2 f7f4f4 f7f3f3 "
           "f6f3f3 f6f2f2 f4eded f2f2f2 f0e7e8 efe7e8 ece0e1 e9e9eb e4d7d8 "
           "e3d8d9 e3dada ded7d7 d7d0d0 d2c8c8 d0c7c7 ccbcbd c9bfbf c7b9ba "
           "c3b8b8").split():
    COLOR[_h] = "od_text"
for _h in ("c8c8c8 d0d0d0 d8c9ca d3c6c7 c9b9ba c4b9b9 e1cbcc 8e8e9a 8c8e98 "
           "8a8d98 858792 b4b5b9 aeb0b9 aaaaaa 888888 444444 b8a0a2 af9395 "
           "a38e8f a38b8d 988586").split():
    COLOR[_h] = "od_text_dim"
for _h in ("9c9ca8 9c8a8b 7c7c88 7c828e 85878e 5c6068 ac9a9b ae9a9b af9c9d "
           "a18d8e a0888a 937f80 947c7e 937b7b 866c6e 806b6c 92949e 88898e "
           "6c707c 6c6c78 76777c 777984 7a808c 6b717d 3c3c46 967d7d 725f60").split():
    COLOR[_h] = "od_text_muted"
for _h in "705c5c b98a8e 8b6467 896265 755a5c".split():
    COLOR[_h] = "od_text_faint"
for _h in ("ff6b6b b89496 b58e91 e5a0a6 e8a1a7 e8a0a6 ffb4b8 ff9e9e ff9a9a "
           "ff8a8a ff737d ff6b74 e05b5b f13f4b").split():
    COLOR[_h] = "od_accent_bright"
for _h in "5bd097 22c9cb 00c8e0 82f3f4 10b981 9fd6ac bfe7cb c9f7d6".split():
    COLOR[_h] = "od_live"
for _h in "ffa500 e8c989 d8c79a d3a66a f0d2a8".split():
    COLOR[_h] = "od_warn"
COLOR["ff4444"] = "od_error"
for _h in ("1a1414 191414 171212 170e0e 110e0e 0e0b0b 0e0909 151518 141010 "
           "0d0d0d 100909 07130b 000000").split():
    COLOR[_h] = "od_on_accent"

# -- surfaces: rounded / glass cards and phone page grounds -> od_ planes -----
RAISED = ("bg_cs_glass_card bg_cs_glass_card_sm bg_skin_slot_preview "
          "bg_skinv2_card bg_skinv2_preview bg_au_card bg_au_card_muted "
          "bg_au_card_fail bg_au_group bg_au_plate bg_preference_card "
          "bg_profile_card_cta bg_s4_card bg_s4h_category_card bg_featured_motd "
          "bg_infrawire_card_glass bg_infrawire_card_glass_soft "
          "bg_infrawire_plan_card background_card bg_card_glass "
          "bg_glass_card_premium mvs_card lg_card csc_card csc_card_hero "
          "pk_card vp_hero_card vp_row_card vp_error_card st_tile st_stat_tile "
          "bg_lh_create_card bg_profile_type_card bg_settings_deck_control "
          "bg_cursorx_card")
PANEL = ("bg_dialog_premium bg_dialog_plantmc rt_panel st_panel cfm_card "
         "csc_sheet csc_popup_compact vs_sheet background_control_editor")
PANE = "bg_profile_card lg_rail"
WELL = ("bg_creation_field bg_deck_icon_tile bg_settings_deck_badge "
        "bg_input_premium bg_cs_input_field bg_skinv2_input csc_input bg_au_well "
        "bg_skinv2_well rt_well pk_field st_icon_well bg_neo_icon_well "
        "pk_glyph_well pw_glyph_well au9_icon_well au9_input au9_terminal "
        "bg_profile_log_console au9_webframe au9_brand_col qs_section_glyph "
        "bg_card_dark")
KEYCAP = "au9_keycap st_key_cap"
ROW = ("bg_settings_row bg_cs_settings_row qs_row vs_row ss_row st_row "
       "bg_side_rail_item bg_au_row bg_shortcut_action_default bg_vp_item "
       "st_rail_item_off")
CHIP = ("bg_cursorx_chip bg_settings_chip bg_settings_chip_active bg_cs_chip "
        "bg_cs_chip_accent bg_au_chip_neutral bg_au_chip_ok bg_infrawire_chip "
        "rt_chip rt_chip_light rt_chip_warn vp_chip_on vp_tag_mc vp_tag_neutral "
        "vp_tag_recommended")
GHOST = ("csc_btn_ghost pe4_btn_ghost st_btn_ghost rt_btn_ghost au9_btn_ghost "
         "cfm_btn_ghost bg_au_ghost bg_infrawire_button_ghost")
PRIMARY = ("pe4_btn_save st_btn_primary rt_btn_primary au9_btn_primary "
           "csc_btn_primary mvs_btn_primary bg_skin_button_primary "
           "bg_install_button_primary bg_neo_btn_primary "
           "bg_infrawire_button_primary bg_save_button cfm_btn_danger "
           "bg_skinv2_danger pe4_btn_danger bg_skin_slot_danger")
ICONBTN = "bg_settings_btn csc_btn_back pe4_back_btn"
SEARCH = "vs_search csc_search lg_search"
BAR = "csc_topbar pe5_topbar au9_statusbar"
LIVEDOT = "csc_accent_bar bg_s4_accent_bar"

SURFACE = {}
for _n in RAISED.split():
    SURFACE[_n] = "od_pane_raised"
for _n in PANEL.split():
    SURFACE[_n] = "od_panel"
for _n in PANE.split():
    SURFACE[_n] = "od_pane"
for _n in WELL.split():
    SURFACE[_n] = "od_well"
for _n in KEYCAP.split():
    SURFACE[_n] = "od_keycap"
for _n in ROW.split():
    SURFACE[_n] = "od_row"
for _n in CHIP.split():
    SURFACE[_n] = "od_chip"
for _n in GHOST.split():
    SURFACE[_n] = "od_btn_ghost"
for _n in PRIMARY.split():
    SURFACE[_n] = "od_btn_primary"
for _n in ICONBTN.split():
    SURFACE[_n] = "od_icon_button"
for _n in SEARCH.split():
    SURFACE[_n] = "od_search_well"
for _n in BAR.split():
    SURFACE[_n] = "od_commandbar"
for _n in LIVEDOT.split():
    SURFACE[_n] = "od_live_dot"
for _n in ("bg_runtime_setup_page bg_cs_page_bg bg_au_page bg_cursorx_stage "
           "bg_cursorx_style bg_skinv2_page pe4_page_bg vh_page au9_page "
           "bg_settings_deck_page").split():
    SURFACE[_n] = "od_window_bg"
SURFACE["bg_au_section"] = "od_pane_header"

# -- text styles, applied ONLY where the token names a TextView role ----------
STYLE = {"AbSectionTitle": "OdText.TitleS", "AbSectionMeta": "OdText.MonoS",
         "AbFeatureTitle": "OdText.TitleS", "AbFeatureLine": "OdText.BodyS",
         "Pe5Title": "OdText.TitleS", "Pe5Label": "OdText.Label",
         "Pe5Desc": "OdText.BodyS", "Pe5Section": "OdText.TitleM",
         "Pe5Meta": "OdText.MonoS", "CsWorldMetaValue": "OdText.Mono",
         "CsWorldMetaLabel": "OdText.Label", "AuEditorRowTitle": "OdText.TitleS",
         "AuSectionLabel": "OdText.Label", "ShortcutSectionHeader": "OdText.Label",
         "Pe5Pill": "OdButton.Chip", "AuChip": "OdButton.Chip",
         "OrbitXChip": "OdButton.Chip", "CsSortChip": "OdButton.Chip",
         "VpFilterChip": "OdButton.Chip", "rt_chip_text": "OdText.Label"}
TEXT_ELEMENTS = ("TextView", "Button", "AppCompatButton", "EditText",
                 "CheckedTextView", "SwitchCompat", "MaterialButton",
                 "AutoCompleteTextView")

# per-file repairs the generic rules cannot express: the last visible relics of
# the previous projects' branding, and the two bare templates.
EXACT = {
    "fragment_cursor_customization.xml": [('android:text="CS"', 'android:text="ORBITX"')],
    "fragment_fastclient_loading.xml": [
        ('android:contentDescription="CS Logo"', 'android:contentDescription="ORBITX"'),
        ('android:text="\u26a1 FastClient by CS Team"',
         'android:text="\u26a1 FastClient \u00b7 ORBITX"'),
        ('android:src="@drawable/bg_hero_minecraft"',
         'android:src="@drawable/orbitx_mark"')],
    "view_logger.xml": [('android:text="cs@launcher: ~/minecraft"',
                         'android:text="orbitx: ~/minecraft"')],
    "dialog_world_details.xml": [
        ('tools:text="/storage/emulated/0/games/Amethyst/.minecraft/saves/New World-"',
         'tools:text="/storage/emulated/0/games/ORBITX/.minecraft/saves/New World-"')],
    "item_version_choice.xml": [
        ('android:background="@drawable/od_row_bg"\n'
         '    android:descendantFocusability="blocksDescendants"\n'
         '    android:layout_marginBottom="6dp"',
         'android:background="@drawable/od_row_bg"\n'
         '    android:descendantFocusability="blocksDescendants"'),
        ('app:tint="#191414"', 'app:tint="@color/od_on_accent"')],
    "fragment_controller_remapper.xml": [
        ('android:background="?attr/colorBgApp"',
         'android:background="@drawable/od_window_bg"'),
        ('android:textColor="?android:attr/textColorPrimary"',
         'android:textColor="@color/od_text"')],
    "item_centered_textview.xml": [
        ('android:gravity="center"/>',
         'android:gravity="center"\n    android:textColor="@color/od_text"\n'
         '    android:textSize="@dimen/od_text_body_s" />')],
    "item_centered_textview_large.xml": [
        ('android:gravity="center"\n    android:minHeight="48dp"/>',
         'android:gravity="center"\n    android:minHeight="@dimen/od_row_h"\n'
         '    android:textColor="@color/od_text"\n'
         '    android:textSize="@dimen/od_text_title_s" />')]}

ANY_HEX = re.compile(r'"#([0-9A-Fa-f]{6})"')


def style_kind(text, idx):
    head = text.rfind("<", 0, idx)
    if head < 0:
        return None
    tag = text[head:text.find(">", head)]
    for el in TEXT_ELEMENTS:
        if re.match(r"<\s*(androidx\.appcompat\.widget\.)?" + el + r"\b", tag):
            return el
    return None


def fix_dp(text):
    """Retire the density-scaling idiom. Safe on ANY layout, so it is applied
    tree-wide: a file that already carried od_ tokens can still hold a stray
    _NNsdp, and those resolve only because the sdp library happens to be on the
    classpath — which is not a reason to keep scaling a pane's furniture."""
    def rep(m):
        n = int(m.group(1))
        return "@dimen/" + DP[n] if n in DP else BIG.get(n, "%ddp" % n)
    return re.sub(r"@dimen/_(\d+)sdp\b", rep, text)


def sweep(text):
    """One layout: the four phone idioms -> od_ tokens."""
    text = fix_dp(text)

    text = re.sub(r"@dimen/_([\d.]+)ssp\b",
                  lambda m: "@dimen/" + SP.get(m.group(1), "od_text_body"), text)

    def rep_col(m):
        n = COLOR.get(m.group(2).lower())
        return 'android:%s="@color/%s"' % (m.group(1), n) if n else m.group(0)
    text = re.sub(r'android:(textColor|textColorHint|textColorHighlight)='
                  r'"(#[0-9A-Fa-f]{6})"', rep_col, text)

    def rep_surf(m):
        to = SURFACE.get(m.group(1))
        return "@drawable/" + to if to else m.group(0)
    text = re.sub(r"@drawable/([A-Za-z0-9_]+)", rep_surf, text)

    out, pos = [], 0
    for m in re.finditer(r'style="@style/([A-Za-z0-9_.]+)"', text):
        to = STYLE.get(m.group(1))
        if to and style_kind(text, m.start()):
            out.append(text[pos:m.start()])
            out.append('style="@style/%s"' % to)
            pos = m.end()
    out.append(text[pos:])
    return "".join(out)


def converge(text):
    """The same value -> role mapping, for files the sweep did not touch."""
    return ANY_HEX.sub(
        lambda m: '"@color/%s"' % COLOR[m.group(1).lower()]
        if m.group(1).lower() in COLOR else m.group(0), text)


def targets():
    """Layouts not yet on the language: those carrying no od_ token."""
    return [n for n in sorted(os.listdir(LAYOUT)) if n.endswith(".xml")
            and "od_" not in open(os.path.join(LAYOUT, n), encoding="utf-8").read()]


# -----------------------------------------------------------------------------
# GATES
# -----------------------------------------------------------------------------
SKIP_DIRS = {"values", "layout", "menu", "xml", "anim", "animator",
             "transition", "raw", "font", "navigation", "interpolator"}
PREFIX = ("Widget.", "TextAppearance.", "Theme.", "ThemeOverlay.", "Base.",
          "Platform.", "android:")
REFS = (("drawable", r"@drawable/([A-Za-z0-9_.]+)"),
        ("color", r"@color/([A-Za-z0-9_.]+)"),
        ("style", r"@style/([A-Za-z0-9_.]+)"),
        ("dimen", r"@dimen/([A-Za-z0-9_.]+)"))


def resource_index():
    draw = set()
    for d in os.listdir(RES):
        p = os.path.join(RES, d)
        if not os.path.isdir(p) or d in SKIP_DIRS:
            continue
        if d.startswith(("drawable", "mipmap")) or d == "color":
            for f in os.listdir(p):
                if not f.startswith("."):
                    draw.add(re.sub(r"\.(9\.)?(xml|png|webp|jpg|jpeg|gif)$", "", f))
    colours, styles, dimens = set(), set(), set()
    for vf in os.listdir(os.path.join(RES, "values")):
        if not vf.endswith(".xml"):
            continue
        t = open(os.path.join(RES, "values", vf), encoding="utf-8").read()
        colours |= set(re.findall(r'<color\s+name="([^"]+)"', t))
        colours |= set(re.findall(r'<item\s+name="([^"]+)"\s+type="color"', t))
        styles |= set(re.findall(r'<style\s+name="([^"]+)"', t))
        dimens |= set(re.findall(r'<dimen\s+name="([^"]+)"', t))
    cdir = os.path.join(RES, "color")
    if os.path.isdir(cdir):
        colours |= {f[:-4] for f in os.listdir(cdir) if f.endswith(".xml")}
    return draw, colours, styles, dimens


def parse_gateway(path):
    """Parse an XML resource the way AAPT2 does.

    AAPT2 is strict about one thing that is easy to get wrong in comment art:
    `The string "--" is not permitted within comments` is a hard failure of
    mergeDebugResources, not a warning — the build stops. So this parses the
    file as-is, comments and all, which is exactly what AAPT2 does. Runs of
    hyphens in a diagram must be '=' (or split) instead.
    """
    import xml.etree.ElementTree as ET
    return ET.parse(path)


def all_res_xml():
    """Every XML resource, not just the layouts. A broken values/ file fails the
    build exactly as hard as a broken layout, so it gets the same check."""
    out = []
    for root, _d, files in os.walk(RES):
        for f in files:
            if f.endswith(".xml"):
                out.append(os.path.join(root, f))
    return sorted(out)


def gate_per_file(names, snap, backup):
    draw, colours, styles, dimens = resource_index()
    pool = {"drawable": draw, "color": colours, "style": styles, "dimen": dimens}
    fails, idloss, unres = [], [], []
    # Every XML resource in res/, not only the swept layouts: a file the sweep
    # never touched is exactly where a structural break would go unnoticed, and
    # values/*.xml failing stops the build just as hard as a layout failing.
    for path in all_res_xml():
        try:
            parse_gateway(path)
        except Exception as e:
            fails.append((os.path.relpath(path, RES), str(e)))
    for name in names:
        path = os.path.join(LAYOUT, name)
        if not os.path.isfile(path):
            continue
        src = open(path, encoding="utf-8").read()
        bf = os.path.join(snap, name + ".ids")
        if os.path.isfile(bf):
            before = {l.strip() for l in open(bf, encoding="utf-8") if l.strip()}
            if before - set(re.findall(r"@\+id/([A-Za-z0-9_]+)", src)):
                idloss.append((name, sorted(before - set(re.findall(
                    r"@\+id/([A-Za-z0-9_]+)", src)))))
        now_refs = {k: set(re.findall(p, src)) for k, p in REFS}
        bpath = os.path.join(backup, name)
        old_refs = ({k: set(re.findall(p, open(bpath, encoding="utf-8").read()))
                     for k, p in REFS} if os.path.isfile(bpath)
                    else {k: set() for k, _ in REFS})
        for kind, _p in REFS:
            for r in now_refs[kind] - old_refs[kind]:
                if r.startswith(PREFIX) or ":" in r:
                    continue
                if r not in pool[kind]:
                    unres.append((name, "@%s/%s" % (kind, r)))
    print("gate 1 · parse failures       :", len(fails))
    for n, e in fails[:5]:
        print("         ", n, e)
    print("gate 1 · files losing ids     :", len(idloss))
    for n, m in idloss[:5]:
        print("         ", n, m)
    print("gate 1 · unresolved new refs  :", len(unres))
    for n, r in unres[:8]:
        print("         ", r, "(in %s)" % n)
    return not (fails or idloss or unres)


def gate_whole_tree():
    def owner_map(t):
        out = {}
        for m in re.finditer(r"<\s*([A-Za-z][A-Za-z0-9_.]*)([^>]*)", t, re.S):
            for i in re.findall(r"@\+id/([A-Za-z0-9_]+)", m.group(2)):
                out.setdefault(i, set()).add(m.group(1))
        return out

    def git(*a):
        return subprocess.run(["git", "-C", REPO] + list(a),
                              capture_output=True, text=True).stdout

    rel = "android/orbitx_launcher/src/main/res/layout"
    base = git("rev-parse", "origin/main").strip() or git("rev-parse", "HEAD").strip()

    # only an id something CASTS matters: a View handed to a method that takes a
    # View is fine whatever element it is.
    cast_ids, CAST = set(), re.compile(
        r"\(\s*([A-Za-z_][A-Za-z0-9_.]*)\s*\)\s*[^;]*?R\.id\.([A-Za-z0-9_]+)")
    for root, _d, files in os.walk(
            os.path.join(REPO, "android/orbitx_launcher/src/main/java")):
        for jf in files:
            if jf.endswith(".java"):
                s = open(os.path.join(root, jf), encoding="utf-8", errors="ignore").read()
                cast_ids |= {i for _t, i in CAST.findall(s)}

    changed = []
    for f in git("ls-tree", "-r", "--name-only", base).split():
        if not (f.startswith(rel) and f.endswith(".xml")):
            continue
        old, cur = git("show", base + ":" + f), os.path.join(REPO, f)
        if not old or not os.path.isfile(cur):
            continue
        b, c = owner_map(old), owner_map(open(cur, encoding="utf-8").read())
        for i in sorted(set(b) & set(c)):
            if b[i] != c[i] and i in cast_ids:
                changed.append((os.path.basename(f), i, sorted(b[i]), sorted(c[i])))

    draw, colours, styles, dimens = resource_index()
    pool = {"drawable": draw, "color": colours, "style": styles, "dimen": dimens}
    unres, seen = [], set()
    for name in sorted(os.listdir(LAYOUT)):
        if not name.endswith(".xml"):
            continue
        t = open(os.path.join(LAYOUT, name), encoding="utf-8").read()
        for kind, pat in REFS:
            for r in set(re.findall(pat, t)):
                if r.startswith(PREFIX) or ":" in r or r in seen:
                    continue
                if r not in pool[kind]:
                    seen.add(r)
                    unres.append((name, "@%s/%s" % (kind, r)))
    print("gate 2 · ids cast in Java      :", len(cast_ids))
    print("gate 2 · type changes that crash:", len(changed))
    for f, i, bt, ct in changed[:8]:
        print("         ", f, i, "|".join(bt), "->", "|".join(ct))
    print("gate 2 · unresolved resources  :", len(unres))
    for n, r in unres[:8]:
        print("         ", r, "(in %s)" % n)
    return not (changed or unres)


def main():
    gate_only = "--gate" in sys.argv
    names = targets()
    print("=" * 68)
    print("ORBIT Desktop layout migration")
    print("  res     :", RES)
    print("  targets : %d layouts not yet on the od_ language" % len(names))
    print("=" * 68)
    if "--list" in sys.argv:
        for n in names:
            print("  " + n)
        return 0

    work = tempfile.mkdtemp(prefix="od-migrate-")
    snap, backup = os.path.join(work, "idsnap"), os.path.join(work, "backup")
    os.makedirs(snap)
    os.makedirs(backup)
    for name in names:
        t = open(os.path.join(LAYOUT, name), encoding="utf-8").read()
        with open(os.path.join(snap, name + ".ids"), "w", encoding="utf-8") as fh:
            fh.write("\n".join(sorted(set(re.findall(r"@\+id/([A-Za-z0-9_]+)", t)))))

    if not gate_only:
        rewritten = 0
        for name in sorted(os.listdir(LAYOUT)):
            if not name.endswith(".xml"):
                continue
            path = os.path.join(LAYOUT, name)
            src = open(path, encoding="utf-8").read()
            # fix_dp and converge are safe on every layout, so they run on the
            # whole tree; sweep (surfaces, styles) only on the target set —
            # rewriting a surface that already carries an od_ token would undo
            # the fuller treatment that file already has.
            out = fix_dp(converge(src))
            if name in names:
                out = converge(sweep(out))
            for a, b in EXACT.get(name, []):
                out = out.replace(a, b)
            if out != src:
                with open(os.path.join(backup, name), "w", encoding="utf-8") as fh:
                    fh.write(src)
                with open(path, "w", encoding="utf-8") as fh:
                    fh.write(out)
                rewritten += 1
        print("passes  : %d layouts rewritten" % rewritten)

    print()
    ok = gate_per_file(names, snap, backup)
    print()
    ok = gate_whole_tree() and ok
    print()
    print("=" * 68)
    print("MIGRATION GATES:", "PASS" if ok else "FAIL")
    print("=" * 68)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
