package com.orbitx.launcher.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Mojang piston-meta access: the version manifest, per-version json, the client jar,
 * the asset index and the content-addressed asset objects.
 *
 * These are the same public endpoints PojavLauncher / HMCL / FCL read; nothing here
 * requires authentication, which is what makes offline play possible.
 */
object MojangApi {
    const val VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    const val RESOURCES = "https://resources.download.minecraft.net"
    const val LIBRARIES = "https://libraries.minecraft.net"

    data class VersionSummary(
        val id: String,
        val type: String,
        val url: String,
        val releaseTime: String,
    )

    fun fetchManifest(): List<VersionSummary> {
        val txt = Downloader.getText(VERSION_MANIFEST, 45_000)
        val root = JSONObject(txt)
        val arr = root.optJSONArray("versions") ?: JSONArray()
        val out = ArrayList<VersionSummary>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out += VersionSummary(
                id = o.optString("id"),
                type = o.optString("type"),
                url = o.optString("url"),
                releaseTime = o.optString("releaseTime"),
            )
        }
        return out
    }

    /** Download & persist a version json, returning the parsed form. */
    fun installVersionJson(id: String, jsonUrl: String): VersionJson {
        val dest = OrbitPaths.versionJson(id)
        Downloader.fetch(jsonUrl, dest)
        return VersionJson.parse(id, JSONObject(dest.readText()))
    }

    /**
     * Resolve a version including its `inheritsFrom` parent chain
     * (Fabric/Forge profile json inherits the vanilla version).
     */
    fun resolveWithParents(id: String): VersionJson {
        val child = VersionJson.parse(id, JSONObject(OrbitPaths.versionJson(id).readText()))
        val parentId = child.inheritsFrom ?: return child
        val parentFile = OrbitPaths.versionJson(parentId)
        if (!parentFile.isFile) {
            throw IllegalStateException("parent version $parentId is not installed")
        }
        val parent = VersionJson.parse(parentId, JSONObject(parentFile.readText()))
        return merge(parent, child)
    }

    /**
     * Merge a child profile onto its parent the way the official launcher does:
     * child mainClass wins, argument lists are concatenated (parent first),
     * libraries are unioned with the child's overriding on duplicate coordinates.
     */
    private fun merge(parent: VersionJson, child: VersionJson): VersionJson {
        val merged = LinkedHashMap<String, Library>()
        parent.libraries.forEach { merged[it.name] = it }
        child.libraries.forEach { merged[it.name] = it }
        return VersionJson(
            id = child.id,
            mainClass = child.mainClass.ifBlank { parent.mainClass },
            inheritsFrom = child.inheritsFrom,
            assetsIndexId = child.assetsIndexId ?: parent.assetsIndexId,
            assetsIndexUrl = child.assetsIndexUrl ?: parent.assetsIndexUrl,
            clientUrl = child.clientUrl ?: parent.clientUrl,
            clientSha1 = child.clientSha1 ?: parent.clientSha1,
            clientSize = if (child.clientSize > 0) child.clientSize else parent.clientSize,
            libraries = merged.values.toList(),
            librariesRaw = child.librariesRaw ?: parent.librariesRaw,
            gameArgs = parent.gameArgs + child.gameArgs,
            jvmArgs = parent.jvmArgs + child.jvmArgs,
            legacyMinecraftArguments = child.legacyMinecraftArguments ?: parent.legacyMinecraftArguments,
            type = child.type.ifBlank { parent.type },
            javaVersionMajor = child.javaVersionMajor ?: parent.javaVersionMajor,
            raw = child.raw,
        )
    }

    /** Install the client jar for [id]; the version json must already be present. */
    fun installClientJar(v: VersionJson): File {
        val url = v.clientUrl ?: throw IllegalStateException("version ${v.id} has no client download")
        return Downloader.fetch(url, OrbitPaths.versionJar(v.id), v.clientSha1, v.clientSize)
    }

    fun installLibraries(v: VersionJson, onProgress: ((Progress) -> Unit)? = null) {
        val jobs = ArrayList<DownloadJob>()
        for (lib in v.libraries) {
            if (!RuleEvaluator.allowed(lib.rules)) continue
            val rel = lib.artifactPath ?: lib.mavenPath()
            val url = lib.artifactUrl ?: "$LIBRARIES/$rel"
            val dest = File(OrbitPaths.librariesDir, rel)
            jobs += DownloadJob(url, dest, lib.artifactSha1, lib.artifactSize)
        }
        Downloader.fetchAll(jobs, onProgress)
    }

    fun classpathFor(v: VersionJson): List<File> =
        v.libraries.filter { RuleEvaluator.allowed(it.rules) }
            .map { File(OrbitPaths.librariesDir, it.artifactPath ?: it.mavenPath()) }
            .filter { it.isFile }

    /** Download an asset index and every object it references. */
    fun installAssets(v: VersionJson, onProgress: ((Progress) -> Unit)? = null) {
        val indexId = v.assetsIndexId ?: "legacy"
        val indexFile = File(OrbitPaths.assetsIndexesDir, "$indexId.json")
        v.assetsIndexUrl?.let { Downloader.fetch(it, indexFile) }
        if (!indexFile.isFile) {
            OrbitLog.w("no asset index for $indexId; skipping assets")
            return
        }
        val root = JSONObject(indexFile.readText())
        val objects = root.optJSONObject("objects") ?: return
        val jobs = ArrayList<DownloadJob>()
        objects.keys().forEach { key ->
            val o = objects.optJSONObject(key) ?: return@forEach
            val hash = o.optString("hash")
            if (hash.isBlank()) return@forEach
            val sub = hash.substring(0, 2)
            jobs += DownloadJob(
                url = "$RESOURCES/$sub/$hash",
                dest = File(OrbitPaths.assetsObjectsDir, "$sub/$hash"),
                sha1 = hash,
                size = o.optLong("size", 0L),
            )
        }
        Downloader.fetchAll(jobs, onProgress)
    }

    /**
     * Extract the per-OS native bundles of the libraries that declare them.
     * On Android only the linux classifiers are usable; the desktop LWJGL natives
     * are replaced by the Android build at launch time, so their absence is not fatal.
     */
    fun installNatives(v: VersionJson): File {
        val outDir = OrbitPaths.nativesFor(v.id)
        for (lib in v.libraries) {
            if (!RuleEvaluator.allowed(lib.rules)) continue
            if (!lib.name.startsWith("org.lwjgl")) continue
            val nativesKey = lib.natives.keys.firstOrNull { it == "natives-linux" || it == "linux" } ?: continue
            val nat = lib.natives[nativesKey] ?: continue
            if (nat.url.isBlank()) continue
            val jar = File(OrbitPaths.librariesDir, nat.path)
            runCatching {
                Downloader.fetch(nat.url, jar, nat.sha1, nat.size)
                unzipFlat(jar, outDir)
            }.onFailure { OrbitLog.w("natives extract failed for ${lib.name}: ${it.message}") }
        }
        return outDir
    }

    /** Extract every entry of a zip into [dest] (flat, skipping directories). */
    fun unzipFlat(zip: File, dest: File) {
        dest.mkdirs()
        java.util.zip.ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                if (!e.isDirectory) {
                    val name = e.name.substringAfterLast('/')
                    if (name.isNotBlank() && !e.name.startsWith("META-INF")) {
                        val out = File(dest, name)
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                    }
                }
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
    }
}
