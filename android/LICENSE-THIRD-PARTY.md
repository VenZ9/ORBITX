# Third-party attribution — OrbitX Launcher

OrbitX Launcher is a **modified redistribution of Zalith Launcher 1.4.1.4**, which in
turn is a derivative of PojavLauncher. This file records the upstream projects, their
licences, and exactly what was changed here. It is required by the GNU GPL and it is
deliberately explicit: nothing upstream has been stripped or hidden.

## Upstream projects

### Zalith Launcher — the direct base
- Source: https://github.com/ZalithLauncher/ZalithLauncher
- Version imported: **1.4.1.4** (`launcher_version_code=141400`)
- Licence: **GNU GPL-3.0** — full text retained in [`LICENSE`](LICENSE)
- Copyright: the Zalith Launcher contributors (principally MovTery and contributors).

### PojavLauncher — the runtime engine beneath Zalith
- Source: https://github.com/PojavLauncherTeam/PojavLauncher
- Licence: **GNU LGPL-3.0**
- Copyright: PojavLauncherTeam and contributors.

Zalith Launcher carries the whole PojavLauncher Java-side runtime and its control
system forward; OrbitX therefore contains LGPL-3.0-derived code by inheritance. The
`net.kdt.pojavlaunch` namespaces and the `pojavexec` native module originate there.

## What OrbitX changed relative to Zalith Launcher 1.4.1.4

Modification date: **2026-09-20**.

1. **Branding.** App label set to "OrbitX Launcher" (`launcher_name` /
   `launcher_app_name` in `ZalithLauncher/gradle.properties`), and the shipped
   application id changed to `com.orbitx.launcher` (debug variant
   `com.orbitx.launcher.debug`).
2. **Launcher icon.** The upstream icon art was replaced with the supplied OrbitX
   ring artwork: adaptive-icon background/foreground plus the mdpi→xxxhdpi legacy
   and round icons. Generation is reproducible from
   `tools/generate_orbitx_icons.py`.
3. **Colour palette.** `ZalithLauncher/src/main/res/values/colors.xml` and
   `values-night/colors_night.xml` were recoloured to a red palette (primary
   `#D32F2F`, deep surfaces `#7F0000`, highlight `#E53935`/`#E57373`, status bar
   `#B71C1C`). The launcher UI is entirely XML-resource driven, so this one file
   pair repaints the launcher, its dialogs and the in-game control overlay.
4. **Build memory settings.** `gradle.properties` was re-tuned so the build fits a
   2048 MB container (upstream ships `-Xmx4096M`).

### Java package namespaces were deliberately NOT renamed

The shipped **application id** is `com.orbitx.launcher`, but the internal Java
packages remain `com.movtery.zalithlauncher` and `net.kdt.pojavlaunch`.

This is intentional and is the honest engineering trade-off, not an oversight:

- 549 source files reference those namespaces directly;
- the native code resolves classes by **hardcoded path strings** —
  `net/kdt/pojavlaunch/MainActivity`, `net/kdt/pojavlaunch/CriticalNativeTest`,
  `net/kdt/pojavlaunch/Logger$eventLogListener`,
  `com/movtery/zalithlauncher/ui/activity/ErrorActivity` — and registers 17 JNI
  symbols whose names derive from the Java package path;
- renaming the packages would therefore break the JNI bindings and the launcher's
  ability to start a game session.

Holding the package names stable is what keeps the runtime working. This does not
reduce any user-visible OrbitX branding: the app name, icon, colours and shipped
application id are all OrbitX.

## Bundled and downloaded components

These ship inside the APK or are fetched on demand at runtime, and remain under
their own licences. The authoritative terms are in each project's own distribution.

| Component | Role | Licence |
|---|---|---|
| OpenJDK runtimes (`jre-8/17/21/25`) | Android JRE used to run Minecraft | GPL-2.0 **with Classpath Exception** |
| LWJGL 3 (+ GLFW) | OpenGL/GLFW Java bindings | BSD-3-Clause |
| gl4es | OpenGL 1.x → GLES translation | MIT |
| OSMesa / Mesa | software GL / Zink driver | MIT |
| virglrenderer | virtual GPU translation | MIT |
| ANGLE (`libEGL_angle`, `libGLESv2_angle`) | GLES over Vulkan/other | BSD-3-Clause |
| Vulkan freedreno layer | Adreno Vulkan support | MIT |
| OpenAL Soft | audio | LGPL-2.0-or-later |
| caciocavallo / caciocavallo17 | AWT-on-Android bridge | GPL-2.0 with Classpath Exception |
| bytehook | native hooking | Apache-2.0 |
| exp4j | expression parser (upstream fork) | Apache-2.0 |
| xz (org.tukaani) | LZMA/XZ decompression | Public domain |
| Gson | JSON | Apache-2.0 |
| toml4j | TOML parsing | MIT |
| OkHttp | HTTP | Apache-2.0 |
| EventBus | in-app events | Apache-2.0 |
| Glide | image loading | BSD-3 / MIT / Apache-2.0 |
| HtmlCleaner | HTML cleaning | BSD-3-Clause |
| StringFog | string obfuscation plugin | Apache-2.0 |
| AndroidX / Material Components | UI toolkit | Apache-2.0 |
| TouchController proxy client | third-party touch controller bridge | see upstream |

**Minecraft itself and its assets are not redistributed here.** The launcher
downloads the game client, libraries and assets from Mojang's servers at first run,
under the user's own account terms. OrbitX is not affiliated with, endorsed by, or
associated with Mojang Studios or Microsoft.

## Licence of the resulting project

Because it is built on **GPL-3.0** Zalith Launcher (and, through it, LGPL-3.0
PojavLauncher), the combined work distributed in `android/` is licensed
**GNU GPL-3.0**. The full text is in [`LICENSE`](LICENSE). Anyone redistributing a
build from this directory must keep that licence, these notices, and must publish
the corresponding source.

The web application that lives elsewhere in this repository is a separate work and
is not covered by this file.

## OrbitX UI modifications (2026-09-20)

This is a UI-only round: no runtime, launch, JRE, native-bridge, GL-translation or
control-overlay behaviour was changed. Upstream's copyright notices and the GPL-3.0
text remain intact and unmodified.

**1. Upstream remote-notice popup removed.**
`net/kdt/pojavlaunch/LauncherActivity` called
`com.movtery.zalithlauncher.feature.notice.CheckNewNotice`, which fetched
`launcher_notice.json` from the `ZalithLauncher/Zalith-Info` GitHub repository and
rendered it in the `notice_layout` view. Its current content is a notice about the
Zalith Launcher Discord server being discontinued, and it is configured to show on
first launch. Removed: the `checkNotice()` and `setNotice()` methods, the
`checkNotice`/`noticeAnimPlayer` fields, the `notice_layout` subtree and its
`DraggableViewWrapper` drag binding, the `notice_got_button` click handler, and the
`noticeCheck` / `noticeNumbering` / `noticeDefault` preference keys in
`AllSettings`. The launcher now opens directly on the home screen. The upstream
automatic update check was also unsubscribed from the launch path for the same
reason (a stale downloaded package is still reported from Settings).

**2. Launcher shell and home screen reorganised.**
`res/layout/activity_launcher.xml`: the header was restructured from a bare title
plus two loose icons into a brand block (mark + wordmark + product line) and a
translucent action pill holding the download and settings buttons.
`res/layout/fragment_launcher.xml`: upstream's composition (one full-height
vertical action list on the left, account/version/Play rail on the right) was
replaced with a different hierarchy - a titled "Quick actions" card containing a 2x2
tile grid with share-logs spanning beneath it, and an "Instance" block ordering
instance selector, profile gear, account and the primary Play action. All view ids
referenced by `MainMenuFragment` are preserved.

**3. Secondary screens reorganised.**
Settings and Downloads: the category tab rail moved from the left edge to the right
edge (indicators and shadow flipped to match). About: the info pager and the
operate/action panel swapped sides and the split guideline moved from 0.69 to 0.31.
Version manager: the section headers moved from a centered title to a left-aligned
overline style, and the shortcuts-vs-management column widths were rebalanced
(0.85 / 1.15). Download/settings rails and the about panel now sit on a rounded
surface instead of a flat overlay colour.

**4. OrbitX visual identity.**
New `res/values/orbitx_ui.xml` (spacing scale, corner radii, brand strings),
`res/drawable/background_header_pill.xml` and
`res/drawable/background_rail.xml`. New `background_header_pill` colour in
`values/colors.xml` and `values-night/colors_night.xml`. Card/row radii raised to
`orbitx_radius_card`. The base theme now sets `colorPrimary` / `colorPrimaryDark` /
`colorAccent` so dialogs, buttons and system chrome inherit the red palette rather
than the framework default accent.

The files above are the complete set of this round's changes; nothing outside
`android/ZalithLauncher/src/main/res/`, `AllSettings.kt` and `LauncherActivity.java`
was touched.

---

## OrbitX round 3 - flat restyle (DroidBridge / HyperLauncher language) and low-end optimisation

Date: 2026-09-21. Base: unchanged from the entry above.

### Reference research (what was actually looked at)

* **DroidBridge Launcher** (`nanowx26/DroidBridgeLauncher`, `ca.dnamobile.droidbridgelauncher`).
  Its public repository was cloned and inspected: it contains **no UI layouts at all** -
  `app/src/main/res/` holds only `drawable/`, `mipmap-*` and three `values*` files, and the
  only UI source is `ui/view/RoundedClipFrameLayout.java` plus one shape-appearance overlay
  declaring an **18dp rounded corner**. The method/`instance`/`settings`/`controls` packages are
  framework-side, not screens. The published store listings' screenshots were analysed for the
  visual language instead.
* **HyperLauncher** (`hollowlauncher/HyperLauncher`, a MojoLauncher fork ultimately based on
  PojavLauncher). Its UI is **Jetpack Compose** (`activity_pojav_launcher.xml` is a bare
  `ComposeView`); its Material 3 theme (`ui/theme/Theme.kt`), colour roles
  (`ui/theme/ColorTheme.kt`) and screen structure were read directly.

### The visual language taken from the references

Both are flat and dark-leaning: depth comes from a **thin 1px outline and a surface value step,
never from drop shadows**; large corner radii (~18-24dp) on cards and stadium/pill shapes on
controls; generous internal padding (12-16dp); no dividers (spacing and value steps separate
regions); a single solid filled primary action; and a compact top bar with a couple of flat
outline icon actions. A single-column list of instances is the main screen, with the primary
action as one dominant element rather than a side rail.

### 1. Home screen recomposed (not merely recoloured)

`res/layout/fragment_launcher.xml` was rewritten. The previous OrbitX composition still kept
upstream's **two-column split** (a full-height action column against a right Play rail) and only
re-ordered its contents. That split is now gone: the screen is one vertical column with three
bands - the current instance as a card, the quick actions as a tile grid inside a scrolling
middle band, and the Play action as the single dominant filled block at the bottom.
`res/layout/activity_launcher.xml` likewise dropped the translucent action pill and the drop
shadow strip, replacing them with a compact flat bar (brand block start, two flat outline icon
actions end) separated from the content by a surface value step. All view ids referenced by
`MainMenuFragment` and `LauncherActivity` are preserved.

### 2. Secondary screens restyled

Settings, Downloads and About: the side rails moved onto a rounded outlined card surface with a
margin, their drop-shadow strips were removed, and the pager's negative end margin was dropped;
the About panel is now a card on the page background. `res/layout/item_version.xml` (the
instance/version list row) moved from the legacy `background_item` to the flat outlined row with
a larger icon and a bolder title.

### 3. New flat design tokens

New `res/values/orbitx_colors.xml` and `res/values-night/orbitx_colors.xml` (surface /
surface_raised / surface_sunken, `orbitx_outline`, the solid accent and its on-colour, text
roles). New drawables: `orbitx_card`, `orbitx_card_raised`, `orbitx_row`,
`orbitx_row_pressed`, `orbitx_tile` (+ normal/pressed/disabled), `orbitx_action_button`
(+ normal/pressed/disabled). `res/values/orbitx_ui.xml` gained an extended spacing scale, the
`orbitx_radius_row` / `_tile` radii, an `orbitx_stroke` hairline token, an `orbitx_row_height`
touch-target token and an `orbitx_action_height` token.

Contrast note: the solid accent is **#D32F2F**, not the #E53935 highlight, because white on
#D32F2F measures 4.98:1 (WCAG AA) while white on #E53935 measures only 4.22:1 and would fail AA
for the Play button label. #E53935 remains the accent for graphics drawn on light surfaces.

### 4. `AnimButton` now honours an XML background

`ui/view/AnimButton.kt` unconditionally replaced its background with a ripple wrapping
`button_background`, so a background declared in XML was silently discarded and the primary
action could not be restyled without editing that class. It now preserves an **explicitly
declared** `android:background` (detected via `obtainStyledAttributes(..., {android.R.attr.background})`
/ `hasValue`) as the ripple content, and keeps the old `button_background` for every button that
does not declare one - so all pre-existing usages render exactly as before.

### 5. Low-end device optimisation

* **Per-ABI JRE asset trim fixed.** The `merge<Variant>Assets` task trimmed architecture-specific
  JRE tarballs using the hardcoded list `listOf("jre-8", "jre-17", "jre-21")`, which **omitted
  `jre-25`** - so an arm64 build still shipped jre-25's unusable `bin-arm.tar.xz` and
  `bin-x86_64.tar.xz`. The list is now discovered from the assets directory. `universal.tar.xz`
  (architecture independent) and the `version` marker (read by `UnpackJreTask.isNeedUnpack`) are
  always kept, and only the target ABI's `bin-<arch>.tar.xz` is retained; with `arch=all` nothing
  is removed.
* **Resource shrinking for release.** Attempted via `release { isShrinkResources = true }`, but AGP
  rejects that combination ("Removing unused resources requires unused code shrinking to be turned
  on") and `isMinifyEnabled` must stay off for the release variant because java.awt is reached
  reflectively.  Resource shrinking therefore remains on the `proguard` variant (which already
  enables minify + shrinkResources); the release variant is left unchanged.
* The GL translation layers were **not** trimmed. All 51 are user-selectable renderer backends
  (gl4es, OSMesa, VirGL, ANGLE, Zink/LTW) exposed through the renderer picker and the renderer
  plugin mechanism. Removing any of them would change or break rendering on a device that selects
  it, so with the launch path being the thing this project must not break, they were all kept.
* The native libraries were **already stripped** upstream (verified with `readelf`: no `.debug_*`
  or `.symtab` sections), so no symbol-stripping saving was available.

No runtime, JRE-provisioning, native-bridge, GL-layer, launch or control-overlay behaviour was
changed by this round.
