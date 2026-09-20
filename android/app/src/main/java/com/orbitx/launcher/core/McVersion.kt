package com.orbitx.launcher.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Mojang version-JSON model plus the pieces of the launch contract that are easy to
 * get subtly wrong: rule evaluation, natives selection and argument templating.
 *
 * The layout follows the upstream client json / PojavLauncher's value model.
 */

/** A single rule entry: allow/disallow, optionally conditioned on OS and/or features. */
data class Rule(
    val action: String,
    val osName: String?,
    val osArch: String?,
    val osVersion: String?,
    val features: Map<String, Boolean>,
) {
    companion object {
        fun parse(o: JSONObject): Rule {
            val os = o.optJSONObject("os")
            val feat = o.optJSONObject("features")
            val features = mutableMapOf<String, Boolean>()
            feat?.keys()?.forEach { k -> features[k] = feat.optBoolean(k, false) }
            return Rule(
                action = o.optString("action", "disallow"),
                osName = os?.optString("name")?.takeIf { it.isNotBlank() },
                osArch = os?.optString("arch")?.takeIf { it.isNotBlank() },
                osVersion = os?.optString("version")?.takeIf { it.isNotBlank() },
                features = features,
            )
        }
    }
}

/**
 * Evaluate an ordered rule list against the Android environment.
 * Minecraft semantics: absent/empty rules => allowed; otherwise the LAST matching
 * rule wins, and no match means disallowed.
 */
object RuleEvaluator {
    /** Feature flags we advertise. Android is treated as a linux-compatible desktop. */
    val features = mapOf(
        "is_demo_user" to false,
        "has_custom_resolution" to true,
        "has_quick_plays_support" to false,
        "is_quick_play_singleplayer" to false,
        "is_quick_play_multiplayer" to false,
        "is_quick_play_realms" to false,
    )

    fun allowed(rules: List<Rule>?): Boolean {
        if (rules.isNullOrEmpty()) return true
        var allowed = false
        for (r in rules) if (matches(r)) allowed = (r.action == "allow")
        return allowed
    }

    private fun matches(r: Rule): Boolean {
        // We present as "linux" so the linux-classified natives/arguments are selected,
        // then substitute the Android build of LWJGL for the desktop one at launch time.
        if (r.osName != null && !r.osName.equals("linux", ignoreCase = true)) return false
        if (r.osArch != null) {
            val arch = if (r.osArch == "x86" || r.osArch == "i386") "x86" else "arm64"
            if (!r.osArch.equals(arch, ignoreCase = true)) return false
        }
        if (r.features.isNotEmpty()) {
            for ((k, v) in r.features) if ((features[k] ?: false) != v) return false
        }
        return true
    }
}

/** A library entry from the version json. */
data class Library(
    val name: String,
    val rules: List<Rule>?,
    val artifactUrl: String?,
    val artifactPath: String?,
    val artifactSha1: String?,
    val artifactSize: Long,
    /** classifier key -> (url, path, sha1, size) for native bundles */
    val natives: Map<String, NativeArtifact>,
) {
    val group: String get() = name.split(":").getOrElse(0) { "" }
    val artifactId: String get() = name.split(":").getOrElse(1) { "" }
    val version: String get() = name.split(":").getOrElse(2) { "" }

    /** maven-style relative path derived from the coordinates when no explicit path is given */
    fun mavenPath(): String {
        val parts = name.split(":")
        val g = parts.getOrElse(0) { "" }.replace('.', '/')
        val a = parts.getOrElse(1) { "" }
        val v = parts.getOrElse(2) { "" }
        val classifier = parts.getOrNull(3)
        val ext = parts.getOrNull(4) ?: "jar"
        val file = if (classifier != null) "$a-$v-$classifier.$ext" else "$a-$v.$ext"
        return "$g/$a/$v/$file"
    }
}

data class NativeArtifact(val url: String, val path: String, val sha1: String?, val size: Long)

/** The parsed version json. */
data class VersionJson(
    val id: String,
    val mainClass: String,
    val inheritsFrom: String?,
    val assetsIndexId: String?,
    val assetsIndexUrl: String?,
    val clientUrl: String?,
    val clientSha1: String?,
    val clientSize: Long,
    val libraries: List<Library>,
    val librariesRaw: JSONArray?,
    /** modern (1.13+) arguments */
    val gameArgs: List<ArgEntry>,
    val jvmArgs: List<ArgEntry>,
    /** legacy (<1.13) single string */
    val legacyMinecraftArguments: String?,
    val type: String,
    val javaVersionMajor: Int?,
    val raw: JSONObject,
) {
    val isLegacy: Boolean get() = legacyMinecraftArguments != null && gameArgs.isEmpty()

    companion object {
        fun parse(id: String, o: JSONObject): VersionJson {
            val libs = mutableListOf<Library>()
            o.optJSONArray("libraries")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val l = arr.optJSONObject(i) ?: continue
                    val dl = l.optJSONObject("downloads")
                    val art = dl?.optJSONObject("artifact")
                    val natives = mutableMapOf<String, NativeArtifact>()
                    dl?.optJSONObject("classifiers")?.let { cls ->
                        cls.keys().forEach { k ->
                            val c = cls.optJSONObject(k) ?: return@forEach
                            natives[k] = NativeArtifact(
                                url = c.optString("url"),
                                path = c.optString("path"),
                                sha1 = c.optString("sha1").takeIf { it.isNotBlank() },
                                size = c.optLong("size", 0L),
                            )
                        }
                    }
                    libs += Library(
                        name = l.optString("name"),
                        rules = parseRules(l.optJSONArray("rules")),
                        artifactUrl = art?.optString("url")?.takeIf { it.isNotBlank() },
                        artifactPath = art?.optString("path")?.takeIf { it.isNotBlank() },
                        artifactSha1 = art?.optString("sha1")?.takeIf { it.isNotBlank() },
                        artifactSize = art?.optLong("size", 0L) ?: 0L,
                        natives = natives,
                    )
                }
            }

            val args = o.optJSONObject("arguments")
            val client = o.optJSONObject("downloads")?.optJSONObject("client")
            val assetIdx = o.optJSONObject("assetIndex")

            return VersionJson(
                id = id,
                mainClass = o.optString("mainClass", "net.minecraft.client.main.Main"),
                inheritsFrom = o.optString("inheritsFrom").takeIf { it.isNotBlank() },
                assetsIndexId = o.optString("assets").takeIf { it.isNotBlank() }
                    ?: assetIdx?.optString("id")?.takeIf { it.isNotBlank() },
                assetsIndexUrl = assetIdx?.optString("url")?.takeIf { it.isNotBlank() },
                clientUrl = client?.optString("url")?.takeIf { it.isNotBlank() },
                clientSha1 = client?.optString("sha1")?.takeIf { it.isNotBlank() },
                clientSize = client?.optLong("size", 0L) ?: 0L,
                libraries = libs,
                librariesRaw = o.optJSONArray("libraries"),
                gameArgs = parseArgs(args?.optJSONArray("game")),
                jvmArgs = parseArgs(args?.optJSONArray("jvm")),
                legacyMinecraftArguments = o.optString("minecraftArguments").takeIf { it.isNotBlank() },
                type = o.optString("type", "release"),
                javaVersionMajor = o.optJSONObject("javaVersion")?.optInt("majorVersion"),
                raw = o,
            )
        }

        fun parseRules(arr: JSONArray?): List<Rule>? {
            if (arr == null || arr.length() == 0) return null
            return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(Rule::parse) }
        }

        private fun parseArgs(arr: JSONArray?): List<ArgEntry> {
            if (arr == null) return emptyList()
            val out = mutableListOf<ArgEntry>()
            for (i in 0 until arr.length()) {
                when (val e = arr.opt(i)) {
                    is String -> out += ArgEntry(listOf(e), null)
                    is JSONObject -> {
                        val rules = e.optJSONArray("rules")?.let { a ->
                            (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(Rule::parse) }
                        }
                        val v = e.opt("value")
                        val values = when (v) {
                            is String -> listOf(v)
                            is JSONArray -> (0 until v.length()).map { v.optString(it) }
                            else -> emptyList()
                        }
                        out += ArgEntry(values, rules)
                    }
                }
            }
            return out
        }
    }
}

/** An argument entry: one or more values gated behind an optional rule list. */
data class ArgEntry(val values: List<String>, val rules: List<Rule>?) {
    fun effective(): List<String> = if (RuleEvaluator.allowed(rules)) values else emptyList()
}
