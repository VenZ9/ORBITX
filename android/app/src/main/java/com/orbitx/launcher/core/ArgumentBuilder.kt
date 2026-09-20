package com.orbitx.launcher.core

import android.os.Build
import java.io.File

/** Everything the argument builder needs about the player + session. */
data class SessionInfo(
    val playerName: String,
    val uuid: String,
    val accessToken: String,
    val userType: String = "msa",
    val xuid: String = "",
    val clientId: String = "",
)

/** Everything the argument builder needs about the machine + install. */
data class LaunchEnvironment(
    val javaHome: File,
    val nativesDir: File,
    val gameDir: File,
    val assetsDir: File,
    val assetsIndexId: String,
    val versionId: String,
    val versionType: String,
    val classpath: List<File>,
    val ramMb: Int,
    val width: Int,
    val height: Int,
)

/**
 * Builds the argv passed to the JVM, for both the modern (1.13+) `arguments` object
 * and the legacy (<1.13) `minecraftArguments` string form.
 *
 * Placeholder substitution mirrors the official launcher's table; getting this wrong
 * is a classic source of "Could not find or load main class" / missing-argument
 * failures, so every documented placeholder is handled explicitly.
 */
object ArgumentBuilder {

    fun buildJvmArgs(v: VersionJson, env: LaunchEnvironment, sess: SessionInfo, renderer: OrbitRenderer): List<String> {
        val out = ArrayList<String>()

        // Modern jvm argument list
        for (entry in v.jvmArgs) {
            for (raw in entry.effective()) out += substitute(raw, v, env, sess)
        }

        // Legacy versions carry no jvm argument list; supply the canonical set.
        if (v.jvmArgs.isEmpty()) {
            out += "-Djava.library.path=${env.nativesDir.absolutePath}"
            out += "-cp"
            out += classpathString(env.classpath)
        }

        // Memory: the launcher owns these, not the version json.
        out.removeAll { it.startsWith("-Xmx") || it.startsWith("-Xms") }
        out += "-Xmx${env.ramMb}M"
        out += "-Xms${minOf(env.ramMb, 512)}M"

        // GL translation layer contract (PojavLauncher-compatible).
        out += "-Dorg.lwjgl.opengl.libname=${renderer.lwjglLibName}"
        // Android's JVM cannot use the desktop GLFW/AWT paths; the bundled LWJGL
        // Android build reads these to size its offscreen surface.
        out += "-Dorg.lwjgl.util.NoChecks=true"
        out += "-Djava.awt.headless=false"
        out += "-Dfml.earlyprogresswindow=false"
        out += "-Dminecraft.launcher.brand=OrbitX"
        out += "-Dminecraft.launcher.version=${BuildConfigVersion.LAUNCHER_VERSION}"
        out += "-Dos.name=Linux"
        out += "-Dos.version=Android-${Build.VERSION.RELEASE}"
        out += "-Dorg.lwjgl.system.allocator=system"
        out += "-XX:ActiveProcessorCount=${Runtime.getRuntime().availableProcessors()}"

        if (Build.SUPPORTED_ABIS.any { it.startsWith("arm") }) {
            out += "-Dos.arch=aarch64"
            out += "-Dorg.lwjgl.system.SharedLibraryExtractPath=${env.nativesDir.absolutePath}"
        }
        return out
    }

    fun buildGameArgs(v: VersionJson, env: LaunchEnvironment, sess: SessionInfo): List<String> {
        val out = ArrayList<String>()
        if (v.isLegacy) {
            val legacy = v.legacyMinecraftArguments.orEmpty()
            legacy.split(' ').filter { it.isNotBlank() }.forEach { out += substitute(it, v, env, sess) }
        } else {
            for (entry in v.gameArgs) {
                for (raw in entry.effective()) out += substitute(raw, v, env, sess)
            }
        }

        // Guarantee the arguments a session cannot start without, in case the
        // version json is unusual or a loader profile omitted them.
        fun ensure(flag: String, value: String) {
            if (out.none { it == flag }) { out += flag; out += value }
        }
        ensure("--username", sess.playerName)
        ensure("--version", env.versionId)
        ensure("--gameDir", env.gameDir.absolutePath)
        ensure("--assetsDir", env.assetsDir.absolutePath)
        ensure("--assetIndex", env.assetsIndexId)
        ensure("--uuid", sess.uuid)
        ensure("--accessToken", sess.accessToken)
        ensure("--userType", sess.userType)
        ensure("--versionType", env.versionType)
        if (out.none { it == "--width" }) {
            out += "--width"; out += env.width.toString()
            out += "--height"; out += env.height.toString()
        }
        return out
    }

    private fun classpathString(files: List<File>) = files.joinToString(":") { it.absolutePath }

    /** Substitute every documented placeholder. */
    fun substitute(
        raw: String,
        v: VersionJson,
        env: LaunchEnvironment,
        sess: SessionInfo,
    ): String {
        var s = raw
        val map = mapOf(
            "auth_player_name" to sess.playerName,
            "version_name" to env.versionId,
            "game_directory" to env.gameDir.absolutePath,
            "assets_root" to env.assetsDir.absolutePath,
            "assets_index_name" to env.assetsIndexId,
            "auth_uuid" to sess.uuid.replace("-", ""),
            "auth_access_token" to sess.accessToken,
            "auth_session" to sess.accessToken,
            "user_type" to sess.userType,
            "user_properties" to "{}",
            "version_type" to env.versionType,
            "natives_directory" to env.nativesDir.absolutePath,
            "launcher_name" to "OrbitX",
            "launcher_version" to BuildConfigVersion.LAUNCHER_VERSION,
            "classpath" to classpathString(env.classpath),
            "classpath_separator" to ":",
            "library_directory" to OrbitPaths.librariesDir.absolutePath,
            "resolution_width" to env.width.toString(),
            "resolution_height" to env.height.toString(),
            "xuid" to sess.xuid,
            "clientid" to sess.clientId,
            "auth_xuid" to sess.xuid,
        )
        for ((k, value) in map) {
            s = s.replace("\${$k}", value)
            // Fabric/Forge profiles occasionally omit the braces.
            s = s.replace("\$$k", value)
        }
        return s
    }
}

/** Kept separate so BuildConfig is not required by the core (it is not unit-testable). */
object BuildConfigVersion {
    const val LAUNCHER_VERSION = "1.0.0"
    const val LAUNCHER_NAME = "OrbitX"
}
