package com.orbitx.launcher.core

import com.orbitx.launcher.data.InstalledVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-level install orchestration, shared by the Versions screen.
 * Every step is idempotent: files are hash-verified by [Downloader], so re-running an
 * install repairs a partial one instead of duplicating work.
 */
object Installer {

    /** Every version folder that looks installed (has a version json). */
    fun installedVersions(): List<InstalledVersion> {
        val root = OrbitPaths.versionsDir
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val json = File(dir, "${dir.name}.json")
            if (!json.isFile) return@mapNotNull null
            InstalledVersion(
                id = dir.name,
                loader = LoaderApi.describe(dir.name),
                hasJar = File(dir, "${dir.name}.jar").isFile,
                folder = dir,
            )
        }.sortedBy { it.id }
    }

    /**
     * Install a vanilla version: version json, client jar, libraries, assets and natives.
     */
    suspend fun installVanilla(
        id: String,
        jsonUrl: String,
        onStage: (String) -> Unit,
        onProgress: ((Progress) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
        onStage("Fetching version manifest entry for $id")
        val v = MojangApi.installVersionJson(id, jsonUrl)
        onStage("Downloading client jar")
        MojangApi.installClientJar(v)
        onStage("Downloading libraries")
        MojangApi.installLibraries(v, onProgress)
        onStage("Downloading assets")
        MojangApi.installAssets(v, onProgress)
        onStage("Extracting natives")
        MojangApi.installNatives(v)
        onStage("Installed $id")
        v
    }

    /**
     * Install a Fabric loader profile on top of an installed vanilla version.
     *
     * The parent must exist before the profile is written: a Fabric profile
     * `inheritsFrom` its vanilla version, so installing it against a missing parent
     * produces a profile that cannot resolve its client jar or its base libraries.
     * The loader version is resolved from the Fabric meta API rather than trusted, and
     * the loader's own libraries are then downloaded from the repositories each library
     * names in the json.
     */
    suspend fun installFabric(
        gameVersion: String,
        loaderVersion: String? = null,
        onStage: (String) -> Unit,
        onProgress: ((Progress) -> Unit)? = null,
    ): String = withContext(Dispatchers.IO) {
        // 1. Parent first.
        if (!OrbitPaths.versionJson(gameVersion).isFile) {
            throw IllegalStateException(
                "Minecraft $gameVersion is not installed. Install the vanilla version " +
                    "first, then add Fabric to it."
            )
        }
        // 2. A loader version that actually exists for this game version.
        onStage("Resolving Fabric loader for $gameVersion")
        val loader = LoaderApi.resolveLoader(gameVersion, loaderVersion)
        val tag = if (loader.stable) "stable" else "beta"
        onStage("Installing Fabric ${loader.version} ($tag) for $gameVersion")

        // 3. Profile json.
        val id = LoaderApi.installFabric(gameVersion, loader.version)
        val v = MojangApi.resolveWithParents(id)

        // 4. The loader's libraries, each from its own repository.
        onStage("Downloading ${v.libraries.size} libraries for $id")
        MojangApi.installLibraries(v, onProgress)
        OrbitLog.i("Fabric ${loader.version} installed as $id")
        onStage("Fabric ${loader.version} installed")
        id
    }

    /** Install Forge by running its installer with a provisioned Java 8 runtime. */
    suspend fun installForge(
        gameVersion: String,
        forgeVersion: String,
        onStage: (String) -> Unit,
        onProgress: ((Progress) -> Unit)? = null,
        onLog: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        if (!OrbitPaths.versionJson(gameVersion).isFile) {
            throw IllegalStateException(
                "Minecraft $gameVersion is not installed. Install the vanilla version first."
            )
        }
        onStage("Provisioning Java 8 for the Forge installer")
        val runtime = GameLauncher.provisionRuntime(8, onStage, onLog)
        val javaExe = File(runtime, "bin/java")
        onStage("Running the Forge installer")
        val id = LoaderApi.installForge(gameVersion, forgeVersion, javaExe, onLog)
        val v = MojangApi.resolveWithParents(id)
        onStage("Downloading ${v.libraries.size} libraries for $id")
        MojangApi.installLibraries(v, onProgress)
        onStage("Forge $forgeVersion installed")
        id
    }

    /** Fetch the public version list for the Versions screen. */
    suspend fun listAvailable(): List<MojangApi.VersionSummary> = withContext(Dispatchers.IO) {
        MojangApi.fetchManifest()
    }

    suspend fun fabricLoaders(gameVersion: String): List<LoaderApi.FabricLoader> =
        withContext(Dispatchers.IO) { LoaderApi.fabricLoaders(gameVersion) }
}
