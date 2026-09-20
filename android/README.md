# OrbitX Launcher (Android)

A Minecraft: Java Edition launcher for Android, written in Kotlin with Jetpack Compose.

This is the native Android application in the repository. The TanStack/React web app in
the repository root is a separate, unrelated project and is untouched by this module.

## Requirements

- JDK 17
- Android SDK: platform 34, build-tools 34.0.0
- Gradle 8.x (or use the wrapper from a machine that has one)
- Optional: an NDK, for the native EGL bridge

## Build

```bash
cd android
ANDROID_HOME=/path/to/sdk gradle assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

The native EGL bridge in `app/cpp/` is only compiled when an NDK is present and the
build is asked for it:

```bash
gradle assembleDebug -Porbitx.native=true
```

Without it the launcher still builds and runs; the session screen reports that the GL
surface is unavailable, and provisioning/launch progress is still shown. A playable
session needs three things this repository does not ship prebuilt (all are fetched or
supplied at runtime):

1. an Android-patched OpenJDK runtime — downloaded automatically on first launch from
   `AngelAuraMC/angelauramc-openjdk-build`;
2. an Android build of LWJGL/GLFW — see `LICENSE-THIRD-PARTY.md`;
3. a GL translation layer (`libgl4es_114.so` / `libOSMesa.so` / `libltw.so`).

## Layout

```
app/src/main/java/com/orbitx/launcher/
  OrbitXApp.kt              Application: storage + logging + state init
  MainActivity.kt           Compose host
  GameActivity.kt           Session host (SurfaceView + EGL bridge + JVM process)
  core/
    OrbitPaths.kt           Canonical storage layout
    OrbitLog.kt             File-backed launcher log
    Downloader.kt           Hash-verified parallel downloads
    MojangApi.kt            Version manifest / json / libraries / assets / natives
    McVersion.kt            Version-JSON model, rule evaluation, arg entries
    LoaderApi.kt            Fabric (meta profiles) and Forge (installer jar)
    ArgumentBuilder.kt      Placeholder substitution, jvm+game argv
    Renderer.kt             GL4ES / VirGL / Zink / ANGLE + runtime catalog/extractor
    GameLauncher.kt         Assembles and spawns the session
    Installer.kt            Install orchestration used by the UI
  data/
    Models.kt               Profile / Control / Layout / Settings
    Store.kt                Single-JSON local storage, exposed as StateFlow
  ui/
    Theme.kt, Common.kt, RootScaffold.kt
    HomeScreen.kt, ProfilesScreen.kt, VersionsScreen.kt
    ControlsScreen.kt, SettingsScreen.kt
```

## Storage

Everything is local; there are no cloud services and no account calls.

- Launcher state: `files/orbitx-state.json`
- Game root: `files/orbitx/` — `versions/`, `libraries/`, `assets/`, `runtimes/`,
  `natives/`, `instances/<version>/`
- Logs: `files/orbitx/logs/orbitx.log` and `game-<version>.log`

## Offline profiles

Profiles are created locally. The UUID is the standard offline identifier — MD5 of
`OfflinePlayer:<name>` with the version/variant bits forced — which is the same value
the game derives for an unauthenticated player, so world data stays consistent with a
desktop client. No Microsoft authentication is needed for offline play.

## Licence

GPL-3.0. See `LICENSE` and `LICENSE-THIRD-PARTY.md`.
