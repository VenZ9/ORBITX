# Third-party components and attribution

OrbitX Launcher is a Minecraft: Java Edition launcher for Android. It is built on the
work of several open-source projects, and this file records what came from where and
under which licence.

## Reference implementations

No source file was copied verbatim from these projects. Their architecture was studied
and the necessary contracts re-implemented:

### PojavLauncher — https://github.com/PojavLauncherTeam/PojavLauncher
- **Licence:** GNU LGPL-3.0 (project); its constituent parts are licensed as listed below.
- **What OrbitX adopts:**
  - The renderer contract. `POJAV_RENDERER`, `LIBGL_ES`, `POJAVEXEC_EGL`,
    `MESA_LOADER_DRIVER_OVERRIDE`, `POJAV_NATIVEDIR`, `AWTSTUB_WIDTH/HEIGHT` and the
    `-Dorg.lwjgl.opengl.libname` JVM property, together with the renderer names
    `opengles2`, `opengles2_5`, `opengles3`, `opengles3_ltw`, `vulkan_zink`.
    These are interface names taken from the LWJGL-on-Android ABI; reusing them is what
    lets OrbitX load the same translation libraries.
  - The launch model: a per-version JSON, a maven-style library tree, a
    content-addressed asset store, `inheritsFrom` profile merging, and launching the
    JVM out-of-process with the official argument vector.
  - The EGL-context handoff design (see `app/cpp/orbitx_egl_bridge.c`).
- **Status:** PojavLauncher was archived in September 2025; its Android line continues
  as Amethyst-Android (AngelAuraMC).

### Zalith Launcher / Zalith Launcher 2 — https://github.com/ZalithLauncher/ZalithLauncher2
- **Licence:** GPL-3.0.
- **What OrbitX adopts:** the UI/UX shape only — a Compose launcher with Home,
  Profiles, Versions and Settings, a profile-per-install model, and a renderer picker.
  No Zalith code or resource is included.

### Fold Craft Launcher — https://github.com/FCL-Team/FoldCraftLauncher
- **Licence:** GPL-3.0.
- **What OrbitX adopts:** feature-level influence only — per-version instance isolation
  (`instances/<version>/`), multi-renderer support including VirGL and Zink, and the
  approach of running the Forge installer jar with a provisioned JVM rather than
  reimplementing the installer. No FCL code is included.

### ZaynLauncher — https://github.com/VenZ9/Zaynlauncher (same author)
- **Licence:** as published by the author.
- **What OrbitX adopts:** the Custom Controls editor pattern — normalized 0..1 control
  geometry, live-state drag/resize via `rememberUpdatedState`, px→dp conversion through
  `LocalDensity`, canvas-bounds clamping, `key(c.id)` item identity, per-layout reset,
  and deterministic placement of new controls.

## Runtime and native components (downloaded at runtime, not distributed here)

These are fetched on demand by the launcher and remain under their own licences:

| Component | Licence | Use |
|---|---|---|
| Android OpenJDK runtimes (`angelauramc-openjdk-build`, successor of `android-openjdk-build-multiarch`) | GPL-2.0 (OpenJDK) | The JRE that runs the game |
| LWJGL 3 | BSD-3-Clause | Java bindings for OpenGL/OpenAL |
| GL4ES | MIT | OpenGL → OpenGL ES translation |
| Mesa 3D (Zink) | MIT | OpenGL → Vulkan translation |
| ANGLE | BSD-3-Clause | GLES over Vulkan |
| VirGL renderer | MIT | Virtualised GL passthrough |
| OpenAL Soft | LGPL-2.1 | Audio |
| Minecraft client, assets and libraries | Mojang EULA | Downloaded from Mojang's public endpoints; not redistributed |
| Fabric loader | Apache-2.0 | Mod loader |
| Minecraft Forge | LGPL-2.1 | Mod loader |

## Build dependencies

AndroidX (Apache-2.0), Jetpack Compose (Apache-2.0), Kotlin (Apache-2.0),
Apache Commons Compress (Apache-2.0), XZ for Java (public domain), Apache Commons IO
(Apache-2.0), kotlinx.coroutines (Apache-2.0).

## Not affiliated

OrbitX Launcher is not affiliated with, endorsed by, or sponsored by Mojang Studios,
Microsoft, PojavLauncher, the Zalith team, or the FCL team. "Minecraft" is a trademark
of Mojang Studios. A valid Minecraft account is required to play online; the launcher's
offline profile mode is intended for local play and for servers that permit it.
