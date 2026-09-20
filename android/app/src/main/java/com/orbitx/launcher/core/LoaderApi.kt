package com.orbitx.launcher.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Mod-loader installation.
 *
 * Fabric is profile-based: the meta API returns a ready made version json that
 * `inheritsFrom` the vanilla version, so installing it is a download plus a merge
 * (handled by MojangApi.resolveWithParents).
 *
 * Forge ships a jar installer; the reliable path (and the one HMCL / FCL take) is to
 * run that installer with the provisioned JVM against the game directory, then adopt
 * the version folder it produces.
 */
object LoaderApi {
    const val FABRIC_META = "https://meta.fabricmc.net/v2"
    const val FORGE_MAVEN = "https://maven.minecraftforge.net"

    data class FabricLoader(val version: String, val stable: Boolean)

    fun fabricLoaders(gameVersion: String): List<FabricLoader> {
        val txt = Downloader.getText("$FABRIC_META/versions/loader/$gameVersion", 30_000)
        val arr = JSONArray(txt)
        val out = ArrayList<FabricLoader>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val l = o.optJSONObject("loader") ?: continue
            out += FabricLoader(l.optString("version"), l.optBoolean("stable", false))
        }
        return out
    }

    /**
     * The loader to install for [gameVersion].
     *
     * The loader version is always resolved from meta.fabricmc.net rather than assumed:
     * loader versions are published per game version, and asking for one that does not
     * exist returns a 404 from the profile endpoint (or, worse, a profile whose
     * libraries do not exist). Preferences: an explicitly requested version if it is
     * real, else the newest stable, else the newest published at all.
     */
    fun resolveLoader(gameVersion: String, requested: String? = null): FabricLoader {
        val loaders = fabricLoaders(gameVersion)
        if (loaders.isEmpty()) {
            throw IllegalStateException(
                "No Fabric loader is published for Minecraft $gameVersion. " +
                    "Fabric supports release versions only; pick a release version. " +
                    "(checked $FABRIC_META/versions/loader/$gameVersion)"
            )
        }
        requested?.takeIf { it.isNotBlank() }?.let { want ->
            loaders.firstOrNull { it.version == want }?.let { return it }
            throw IllegalStateException(
                "Fabric loader $want does not exist for Minecraft $gameVersion. " +
                    "Newest available: ${loaders.first().version}"
            )
        }
        return loaders.firstOrNull { it.stable } ?: loaders.first()
    }

    /**
     * Install a Fabric loader profile for [gameVersion].
     * Returns the generated profile version id (e.g. "fabric-loader-0.15.11-1.21").
     * [loaderVersion] is verified to exist first, and the failure message names the
     * exact url and status when the meta API refuses.
     */
    fun installFabric(gameVersion: String, loaderVersion: String): String {
        // Fail with a precise message before writing anything if the pair is not real.
        resolveLoader(gameVersion, loaderVersion)
        val profileUrl = "$FABRIC_META/versions/loader/$gameVersion/$loaderVersion/profile/json"
        val txt = try {
            Downloader.getText(profileUrl, 30_000)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Fabric profile unavailable for Minecraft $gameVersion, loader $loaderVersion. " +
                    "Tried: $profileUrl -> ${e.message}", e
            )
        }
        val obj = JSONObject(txt)
        val id = obj.optString("id").ifBlank { "fabric-loader-$loaderVersion-$gameVersion" }
        val dest = OrbitPaths.versionJson(id)
        dest.parentFile?.mkdirs()
        dest.writeText(obj.toString(2))
        OrbitLog.i("installed Fabric profile $id (${obj.optJSONArray("libraries")?.length() ?: 0} libraries)")
        return id
    }

    /**
     * Install Forge by running the official installer with the provisioned JVM.
     * [javaExe] must be the `<runtime>/bin/java` of a Java 8+ runtime.
     */
    fun installForge(
        gameVersion: String,
        forgeVersion: String,
        javaExe: File,
        onLog: (String) -> Unit,
    ): String {
        val installerName = "forge-$gameVersion-$forgeVersion-installer.jar"
        // The installer lives under a release-specific path in the Forge maven.
        val candidates = listOf(
            "$FORGE_MAVEN/net/minecraftforge/forge/$gameVersion-$forgeVersion/$installerName",
        )
        val installer = File(OrbitPaths.cacheDir, installerName)
        var ok = false
        for (url in candidates) {
            runCatching { Downloader.fetch(url, installer); ok = true }
        }
        if (!ok || !installer.isFile) {
            throw IllegalStateException("could not download Forge installer for $gameVersion-$forgeVersion")
        }
        OrbitLog.i("running Forge installer $installerName")
        val pb = ProcessBuilder(
            javaExe.absolutePath,
            "-jar", installer.absolutePath,
            "--installClient", OrbitPaths.gameDir.absolutePath,
        )
        pb.directory(OrbitPaths.gameDir)
        pb.redirectErrorStream(true)
        pb.environment()["JAVA_HOME"] = javaExe.parentFile.parentFile.absolutePath
        val proc = pb.start()
        proc.inputStream.bufferedReader().forEachLine { onLog(it) }
        val code = proc.waitFor()
        if (code != 0) throw IllegalStateException("Forge installer exited with $code")
        val expected = "$gameVersion-forge-$forgeVersion"
        val dir = OrbitPaths.versionDir(expected)
        if (dir.isDirectory) return expected
        // Some installer releases name the profile differently; adopt the newest one
        // that mentions forge and the game version.
        val found = OrbitPaths.versionsDir.listFiles()
            ?.filter { it.isDirectory && it.name.contains("forge") && it.name.contains(gameVersion) }
            ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("Forge installed but no profile folder was produced")
        return found.name
    }

    /** Status of an installed loader profile, for display in the UI. */
    fun describe(versionId: String): String = when {
        versionId.contains("fabric", true) -> "Fabric"
        versionId.contains("forge", true) -> "Forge"
        versionId.contains("neoforge", true) -> "NeoForge"
        versionId.contains("quilt", true) -> "Quilt"
        else -> "Vanilla"
    }
}
