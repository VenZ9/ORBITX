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
