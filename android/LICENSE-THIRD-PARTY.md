# Third-party attribution — OrbitX Launcher

OrbitX Launcher is a **modified distribution built on the CS Launcher Plus +
Amethyst Launcher lineage**, which in turn descend from PojavLauncher. This file
records the upstream projects, their licences, and exactly what was changed here.
It is required by the GNU GPL and it is deliberately explicit: nothing upstream
has been stripped or hidden.

## Upstream projects

### CS Launcher Plus — the direct base
- Source: https://github.com/craftstudioteam/CS-LAUNCHER-PLUS
- Licence: **GNU GPL-3.0** — full text retained in [`LICENSE`](LICENSE)
- Copyright: the CS Launcher Plus / Craft Studio authors and contributors.

### Amethyst Launcher — the second base
- Source: https://github.com/AngelAuraMC/Amethyst-Android
- Licence: **GNU LGPL-3.0**
- Copyright: the AngelAuraMC authors and contributors.
- Used as the second code base, per the project's stated build direction, for the
  launcher shell and runtime-provisioning behaviour this tree carries forward.

### PojavLauncher — the runtime engine beneath both
- Source: https://github.com/PojavLauncherTeam/PojavLauncher
- Licence: **GNU LGPL-3.0**
- Copyright: PojavLauncherTeam and contributors.

Both bases carry the PojavLauncher Java-side runtime forward; OrbitX therefore
contains LGPL-3.0-derived code by inheritance. The `net.kdt.pojavlaunch`
namespaces and the `pojavexec` native module originate there.

### Zalith Launcher — dropped, not a base
**Zalith Launcher is no longer part of this project.** The `ZalithLauncher`
module, its Gradle scripts, its libraries and its entire resource tree were
removed from the repository and replaced by the single rebranded module
`android/orbitx_launcher`. No Zalith code ships in the current build.

## What OrbitX changed relative to the CS Launcher Plus + Amethyst base

Modification date: **2026-09-23**.

1. **Module + identity.** One module, `:orbitx_launcher`. Package
   `net.kdt.pojavlaunch`, application id `com.orbitx.launcher` (debug variant
   `com.orbitx.launcher.debug`), label `ORBITX`, compile/targetSdk 34,
   minSdk 21, ABIs `arm64-v8a` and `armeabi-v7a`.
2. **Launcher icon and brand mark.** The upstream icon art was replaced with the
   supplied OrbitX ring artwork: adaptive-icon background/foreground plus the
   mdpi→xxxhdpi legacy and round icons, and an alpha-keyed ring mark for dark
   surfaces. Generation is reproducible from `tools/make_brand_assets.py`.
3. **Colour system.** The violet/silver accent family was replaced throughout
   with a single OrbitX ember accent (`#FF3B30`, with `#FF6B6B` bright and
   `#C62828` deep steps) on warmed near-black surfaces. Red is the default theme.
4. **UI recomposition.** The navigation rail, home stage, instance library and
   About screen were rebuilt as an OrbitX design language rather than a copy of
   the CS Launcher Plus layout.
5. **In-game replay recorder.** A new `net.kdt.pojavlaunch.recorder` package
   captures via `MediaProjection` + `MediaRecorder` with **no on-screen overlay
   while recording**, exporting `.MP4` natively plus `.WebM` and `.GIF`.
6. **Starting-overlay opacity.** The boot-log / settings panel shown while the
   game starts can be dimmed or hidden — Launcher settings → **Starting
   overlay**: `100% · 85% · 70% · 50% · 30% · Hide`.
7. **Bundled renderers.** MobileGlues and LTW ship inside the APK in `jniLibs`
   for both ABIs, so no separate renderer download is required.
8. **First-run onboarding removed.** No welcome popup, no guided tour, no
   overlay — the launcher opens straight into Home.
9. **Community links removed.** All Discord, GitHub and YouTube destinations
   were removed from the app UI.
10. **Remote admin panel removed.** The remote config / announcement panel was
    replaced by inert local stubs; the launcher makes no remote config calls.

### Java package namespaces were deliberately NOT renamed

The shipped **application id** is `com.orbitx.launcher`, but the internal Java
packages remain `net.kdt.pojavlaunch`.

This is intentional and is the honest engineering trade-off, not an oversight:

- a large number of source files reference that namespace directly;
- the native code resolves classes by **hardcoded path strings** —
  `net/kdt/pojavlaunch/MainActivity`, `net/kdt/pojavlaunch/CriticalNativeTest`,
  `net/kdt/pojavlaunch/Logger$eventLogListener` — and registers JNI symbols
  whose names derive from the Java package path;
- renaming the packages would therefore break the JNI bindings and the
  launcher's ability to start a game session.

Holding the package name stable is what keeps the runtime working. This does not
reduce any user-visible OrbitX branding: the app name, icon, colours and shipped
application id are all OrbitX.

## Bundled and downloaded components

These ship inside the APK or are fetched on demand at runtime, and remain under
their own licences. The authoritative terms are in each project's own distribution.

| Component | Role | Licence |
|---|---|---|
| MobileGlues | bundled GL renderer | **LGPL-2.1** |
| LTW (Zink/OSMesa path) | bundled GL renderer | see upstream |
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

Because it is built on **GPL-3.0** CS Launcher Plus (and, through it,
**LGPL-3.0** Amethyst Launcher and PojavLauncher), the combined work distributed
in `android/` is licensed **GNU GPL-3.0**. The full text is in [`LICENSE`](LICENSE).
Anyone redistributing a build from this directory must keep that licence, these
notices, and must publish the corresponding source.

The web application that lives elsewhere in this repository is a separate work and
is not covered by this file.

## Revision history

Rounds 1–4 (2026-09-20 / 2026-09-21) were carried out **against the
`android/ZalithLauncher/` tree, which has since been removed entirely**. Their
records — the notice-popup removal, the early shell and home reorganisations, the
flat restyle and the Quick-actions sizing pass — are retained below only as a
historical log of a tree that no longer exists, and they must not be read as
describing the current `android/orbitx_launcher/` module.

**Round 5 — base swap to CS Launcher Plus + Amethyst, and the OrbitX identity
(2026-09-23).** This is the round that produced the current tree: Zalith dropped,
`android/orbitx_launcher/` created, the ember palette and design system applied
across all surfaces, the navigation rail and screens recomposed, the replay
recorder added, the starting-overlay opacity control added, MobileGlues and LTW
bundled, the Microsoft / ely.by / offline login paths wired, the community links
and remote admin panel removed, and the first-run onboarding system deleted
(the `net.kdt.pojavlaunch.tutorial` package, `PlusWelcomeDialog`,
`CsSoundPlayer` and the Home demo-card plumbing).

### Historical: rounds 1–4 (Zalith-based tree, since removed)

- **Round 1 (2026-09-20)** — upstream remote-notice popup removed; launcher shell
  and home screen reorganised; secondary screens (settings, downloads, about,
  version manager) restructured; first OrbitX visual-identity tokens added.
- **Round 2 (2026-09-20)** — OrbitX UI modifications, as recorded at the time:
  UI-only, no runtime, launch, JRE, native-bridge, GL-translation or
  control-overlay behaviour changed.
- **Round 3 (2026-09-21)** — flat restyle drawing on the DroidBridge and
  HyperLauncher visual languages; home recomposed into a single vertical column;
  flat design tokens introduced; `AnimButton` made to honour an XML background;
  per-ABI JRE asset trim fixed to discover the runtime list dynamically.
- **Round 4 (2026-09-21)** — Quick actions made thumb-friendly: tiles raised to
  64–76dp touch targets, icon and label sizes tokenised, band spacing tightened.
