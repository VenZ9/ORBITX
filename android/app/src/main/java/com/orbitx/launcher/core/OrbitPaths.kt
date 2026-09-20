package com.orbitx.launcher.core

import java.io.File

/**
 * Canonical storage layout for the launcher.
 *
 * Everything is local and file-based (no cloud services):
 *   files/orbitx/                 <- game root ("game directory")
 *     versions/<id>/              <- per-version json + client jar (version isolation)
 *     libraries/                  <- maven-style library tree
 *     assets/objects/<h2>/<hash>  <- content-addressed asset store
 *     assets/indexes/<id>.json
 *     runtimes/<name>/            <- unpacked JRE per name
 *     natives/<id>/               <- extracted LWJGL natives for a version
 *     instances/<version>/        <- isolated save/mod/config dir per version
 */
object OrbitPaths {
    @Volatile private var filesRoot: File? = null

    fun init(filesDir: File) {
        filesRoot = filesDir
        listOf(gameDir, versionsDir, librariesDir, assetsDir, assetsObjectsDir,
            assetsIndexesDir, runtimesDir, nativesDir, instancesDir, cacheDir, logsDir)
            .forEach { it.mkdirs() }
    }

    private val root: File get() = filesRoot ?: error("OrbitPaths.init() not called")

    val gameDir get() = File(root, "orbitx")
    val versionsDir get() = File(gameDir, "versions")
    val librariesDir get() = File(gameDir, "libraries")
    val assetsDir get() = File(gameDir, "assets")
    val assetsObjectsDir get() = File(assetsDir, "objects")
    val assetsIndexesDir get() = File(assetsDir, "indexes")
    val runtimesDir get() = File(gameDir, "runtimes")
    val nativesDir get() = File(gameDir, "natives")
    val instancesDir get() = File(gameDir, "instances")
    val logsDir get() = File(gameDir, "logs")
    val cacheDir get() = File(root, "orbitx-cache")

    fun versionDir(id: String) = File(versionsDir, id)
    fun versionJson(id: String) = File(versionDir(id), "$id.json")
    fun versionJar(id: String) = File(versionDir(id), "$id.jar")
    fun instanceDir(id: String) = File(instancesDir, id).apply { mkdirs() }
    fun runtimeDir(name: String) = File(runtimesDir, name)
    fun nativesFor(id: String) = File(nativesDir, id).apply { mkdirs() }
}
