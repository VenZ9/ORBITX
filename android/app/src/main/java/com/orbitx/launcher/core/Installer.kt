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

    /** Install a Fabric loader profile on top of an installed vanilla version. */
    suspend fun installFabric(
        gameVersion: String,
        loaderVersion: String,
        onStage: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        onStage("Installing Fabric $loaderVersion for $gameVersion")
        val id = LoaderApi.installFabric(gameVersion, loaderVersion)
        // The profile inherits the vanilla version, so its parent must be present.
        if (!OrbitPaths.versionJson(gameVersion).isFile) {
            throw IllegalStateException("install vanilla $gameVersion first")
        }
        val v = MojangApi.resolveWithParents(id)
        onStage("Downloading Fabric libraries")
        MojangApi.installLibraries(v)
        onStage("Fabric $loaderVersion installed")
        id
    }

    /** Install Forge by running its installer with a provisioned Java 8 runtime. */
    suspend fun installForge(
        gameVersion: String,
        forgeVersion: String,
        onStage: (String) -> Unit,
        onLog: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        onStage("Provisioning Java 8 for the Forge installer")
        val runtime = GameLauncher.provisionRuntime(8, onStage, onLog)
        val javaExe = File(runtime, "bin/java")
        onStage("Running the Forge installer")
        val id = LoaderApi.installForge(gameVersion, forgeVersion, javaExe, onLog)
        val v = MojangApi.resolveWithParents(id)
        onStage("Downloading Forge libraries")
        MojangApi.installLibraries(v)
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
