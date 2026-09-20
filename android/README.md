# OrbitX Launcher (Android)

A Minecraft: Java Edition launcher for Android, rebranded and recoloured as
**OrbitX**. This directory is a modified redistribution of **Zalith Launcher
1.4.1.4** (GPL-3.0), which itself carries the **PojavLauncher** runtime
(LGPL-3.0). See [`LICENSE-THIRD-PARTY.md`](LICENSE-THIRD-PARTY.md) for full
attribution and the exact list of changes.

<p align="center">
  <img src="ZalithLauncher/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="140" alt="OrbitX Launcher icon"/>
</p>

## What OrbitX is

| | |
|---|---|
| App name | **OrbitX Launcher** |
| Application ID | `com.orbitx.launcher` (debug: `com.orbitx.launcher.debug`) |
| minSdk / targetSdk | 26 / 34 |
| Runtime | Java Edition via a bundled Android JRE (8 / 17 / 21 / 25) |
| Renderers | GL4ES, VirGL, Zink (OSMesa), ANGLE — selectable per instance |
| Mod loaders | Fabric, Forge, NeoForge, Quilt, OptiFine |
| Controls | PojavLauncher's control system, recoloured red, incl. custom-key buttons |
| Auth | Offline accounts supported; no account needed to launch |

## Layout

```
android/
├── ZalithLauncher/          the app module (Kotlin + Java + XML/Compose UI)
│   ├── src/main/java/       app sources (namespaces retained upstream, see below)
│   ├── src/main/jni/        native bridge, built with the NDK
│   ├── src/main/jniLibs/    prebuilt GL translation layers (.so)
│   ├── src/main/assets/     bundled JREs (8/17/21/25), LWJGL, caciocavallo
│   └── src/main/res/        icons, layouts, strings, colours
├── jre_lwjgl3glfw/          LWJGL/GLFW shim compiled into the APK assets
├── build.gradle.kts         root build
├── gradle.properties        memory-fitted Gradle settings
└── gradlew                  Gradle wrapper (pinned)
```

## Building

Requirements: **JDK 17**, Android SDK with **platform 34**, **build-tools
34.0.0**, and **NDK 25.2.9519653** — the native bridge is compiled with the NDK,
so the build fails at `externalNativeBuild` without it.

```bash
export ANDROID_HOME=/opt/android-sdk          # or your SDK location
export JAVA_HOME=/path/to/jdk-17

cd android
./gradlew assembleDebug -Darch=arm64          # or -Darch=all, -Darch=arm, ...
```

The APK lands in `ZalithLauncher/build/outputs/apk/debug/`.

`-Darch=<abi>` selects the ABI (`all`, `arm`, `arm64`, `x86`, `x86_64`). For a
device-only build `arm64` keeps the APK far smaller.

### Memory

`gradle.properties` is deliberately tuned for a **2048 MB container with no
swap** — upstream ships `-Xmx4096M`, which gets the daemon OOM-killed
immediately. These settings bound the *total* JVM footprint (heap 768 MB,
metaspace 320 MB, explicit `CompressedClassSpaceSize`) because the Gradle
daemon grows to ~1.4 GB RSS while compiling this project, and Jetifier is off
since every dependency is already AndroidX. On a normal 8 GB+ machine you can
raise `org.gradle.jvmargs` back toward `-Xmx4096M` for a faster build.

### Java package namespace

The shipped **application ID is `com.orbitx.launcher`**, but the internal Java
packages deliberately remain `com.movtery.zalithlauncher` and
`net.kdt.pojavlaunch`. 549 sources reference those namespaces and the native
code resolves classes by hardcoded path strings, so renaming them would break
the JNI bindings and game launching. All user-visible branding — app name,
icon, colours, application ID — is OrbitX. This is explained in full in
[`LICENSE-THIRD-PARTY.md`](LICENSE-THIRD-PARTY.md).

## Controls

The control overlay is PojavLauncher's: movement D-pad, jump/sneak/sprint,
attack/use, inventory, drop, chat, pause and the hotbar row — recoloured to the
OrbitX red palette. Custom-key buttons are supported: in the controls editor
you can add a control and bind it to any keyboard key or mouse action.
Layouts are stored normalised `0..1`, with Default / PvP / Custom presets and
separate portrait and landscape arrangements.

## Licence

**GNU GPL-3.0** — see [`LICENSE`](LICENSE), with third-party attribution in
[`LICENSE-THIRD-PARTY.md`](LICENSE-THIRD-PARTY.md). Because this is a
derivative of GPL-3.0 Zalith Launcher, any redistributed build must keep these
notices and publish its source.

Not affiliated with, endorsed by, or associated with Mojang Studios or
Microsoft. Minecraft is a trademark of Mojang Studios.
