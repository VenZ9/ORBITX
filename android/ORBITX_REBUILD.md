# ORBITX — Rebuild Notes

ORBITX is now built **from scratch on the CS Launcher Plus + Amethyst Launcher
lineage**. The previous **Zalith Launcher base has been dropped completely** —
the `ZalithLauncher` module, its Gradle scripts, its libraries and its entire
resource tree were removed from the repository and replaced by a single
rebranded launcher module.

```
android/
  orbitx_launcher/        ← the launcher (ex-CS Launcher Plus, Amethyst/Pojav lineage)
  build.gradle            ← root build script
  settings.gradle         ← rootProject.name = 'ORBITX'
  gradlew / gradle/       ← wrapper
```

Module: `:orbitx_launcher` · package `net.kdt.pojavlaunch` ·
application id `com.orbitx.launcher` (debug: `com.orbitx.launcher.debug`) ·
label `ORBITX` · compileSdk/targetSdk 34 · minSdk 21 ·
ABIs `arm64-v8a`, `armeabi-v7a`.

## What was implemented

### 1. Cleaner UI on the OrbitX identity
The CS Launcher Plus visual language is kept where it already works — dark
charcoal surfaces, rounded cards/pills, the persistent **left navigation rail**
and a home screen centred on the player/skin, the selected instance and one
prominent launch action — and rebuilt around one accent colour.

- The old violet/metallic-silver accent was replaced **everywhere** with OrbitX
  red (`#FF3B30`, with `#D32F2F` / `#FF6B6B` as the deep/light steps).
- Rebranding is global: app name and short name (`ORBITX`), `rootProject.name`,
  `applicationId`, launch identity, About screen, boot-log header, cursors,
  the managed-files detector and every user-facing string that still said
  "CS Launcher Plus", "CS Studio", "CS Client" or "craftstudioteam".
- Home is deliberately simplified for **low-end hardware**: one-tap launch,
  no nested navigation, and the boot-log console capped to a corner panel
  (36 % of the screen) instead of covering the game.
- The remote Firebase admin panel (announcements, remote update checks and a
  sponsorship gate) was removed; it is replaced by inert local stubs, so the
  launcher is fully self-contained and makes no remote config calls.

### 2. In-game replay / recording mod — no overlay while recording
New `net.kdt.pojavlaunch.recorder` package:

| Class | Role |
|---|---|
| `RecorderManager` | process-wide entry point, consent round-trip, format choice |
| `ScreenRecorder` | `MediaProjection` + `VirtualDisplay` + `MediaRecorder` capture |
| `RecorderService` | silent `mediaProjection` foreground service (required on Android 14+) |
| `VideoExporter` | re-mux to another container, or re-encode to GIF |
| `GifEncoder` | built-in GIF89a writer (LZW), no third-party dependency |
| `RecorderFormat` | MP4 (H.264+AAC) / WebM (VP8+Vorbis) |

- It is driven from the existing **in-game drawer** (“Record / Replay”), not
  from a floating button.
- **Nothing is drawn on top of the game while capturing** — no recorder button,
  badge, timer or indicator — so a clip contains only the game. The only trace
  of a running capture is a silent, minimal `IMPORTANCE_MIN` notification, which
  is a platform requirement of a foreground service.
- Capture is published straight to the container, so **`.MP4` is produced
  natively**; a picker then offers **`.WebM`** as a second container (lossless
  re-mux, no re-encode) and **`.GIF`** as a third option.
- Clips land in a user-visible `OrbitX/Recordings` folder. Bitrate scales with
  the encoded resolution and capture is capped to 1080p-class output — tuned for
  low-end devices.

### 3. Starting-overlay opacity (cogwheel / settings panel)
The boot-log & settings panel shown while the game starts can now be **dimmed or
hidden entirely** — Launcher settings → **Starting overlay**:
`100% · 85% · 70% · 50% · 30% · Hide`.
Implemented as `BootLogOverlay.setColumnOpacity()` plus
`MainActivity.applyLaunchOverlayOpacity()`; “Hide” removes it from the screen
before the first frame is presented.

### 4. Renderers bundled in the app — no separate downloads
**MobileGlues** and **LTW** are shipped inside the APK in `jniLibs` for both
arm64-v8a and armeabi-v7a:

```
lib/arm64-v8a/libmobileglues.so   lib/arm64-v8a/libltw.so
lib/armeabi-v7a/libmobileglues.so lib/armeabi-v7a/libltw.so
(+ libmobileglues_info_getter.so for both ABIs)
```

Astral's renderer picker already probes the app's native library directory
(`getCompatibleRenderers` → `System.loadLibrary`), so both renderers appear in
the list and work with **no external download step**.

### 5. Login methods
- **Microsoft** — MSAL interactive flow (`MicrosoftAuthActivity` +
  `MicrosoftLoginFragment`, Minecraft title id `00000000402b5328`).
- **ely.by** — `ElybyLoginFragment`.
- **Local / offline** — `LocalLoginFragment` (`SelectAuthFragment` picker).
- The two default-locale login strings that were missing upstream
  (`login_microsoft`, `login_error_invalid_username`) were added.

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk17
cd android && ./gradlew :orbitx_launcher:assembleDebug
```

Verified clean: `BUILD SUCCESSFUL` — APK at
`android/orbitx_launcher/build/outputs/apk/debug/orbitx_launcher-debug.apk`.

> Toolchain note: this environment caps the cgroup at 2 GB, so
> `gradle.properties` pins `-Xmx1152m` with `--max-workers=1`. Raise both on a
> normal machine for a much faster build.

## Credits
Launcher base: CS Launcher Plus / Amethyst Launcher (PojavLauncher lineage) —
see `LICENSE-THIRD-PARTY.md`. MobileGlues and LTW renderers ship under their own
upstream licences.
