package com.orbitx.launcher.core

import org.json.JSONObject
import java.io.File

/**
 * Whether the native GL bridge is actually in this APK.
 *
 * The bridge is only compiled when the build runs with `-Porbitx.native=true` and an NDK
 * is present. Without it there is no EGL surface for the JVM's GL calls, so a session
 * cannot present anything: the launcher must say so up front rather than starting a JVM
 * that dies several screens later with an opaque driver error.
 *
 * Library identity caveat: `System.loadLibrary` caches a successful load per class loader,
 * and the native methods are declared on GameActivity. So this probe runs against this
 * class and GameActivity performs its own load; the two agree because the library is
 * either present in the APK for every caller or absent for all of them.
 */
object NativeBridge {
    const val LIBRARY = "orbitx_bridge"

    val available: Boolean by lazy {
        runCatching { System.loadLibrary(LIBRARY); true }.getOrElse {
            OrbitLog.i("native bridge $LIBRARY not present in this build")
            false
        }
    }

    val missingHint: String =
        "This build has no native EGL bridge ($LIBRARY.so, needs an NDK build with " +
            "-Porbitx.native=true). Provisioning and the JVM's console output still work, " +
            "but no rendered game surface is possible."
}

/** One pre-launch assertion. */
data class LaunchCheck(
    val label: String,
    val ok: Boolean,
    val detail: String,
    /** Blocks the launch (vs. a warning that only degrades the session). */
    val blocking: Boolean = true,
)

/**
 * What the launcher found when it inspected an instance.
 *
 * [canLaunch] gates the start button; [summary] is the message shown to the user. The
 * whole point of this type is that starting a session can never silently do nothing: the
 * caller always has a report to display, whether the run begins or not.
 */
data class LaunchReport(
    val checks: List<LaunchCheck>,
    val versionId: String,
    val profileId: String,
    val profileName: String,
) {
    val blockingIssues: List<LaunchCheck> get() = checks.filter { !it.ok && it.blocking }
    val warnings: List<LaunchCheck> get() = checks.filter { !it.ok && !it.blocking }
    val canLaunch: Boolean get() = blockingIssues.isEmpty()

    /** First problem worth showing, preferring blocking issues. */
    val headline: String?
        get() = (blockingIssues.firstOrNull() ?: warnings.firstOrNull())?.detail

    val nativeBridgeAvailable: Boolean get() = NativeBridge.available

    fun summary(): String = when {
        blockingIssues.isNotEmpty() ->
            "Cannot start:\n" + blockingIssues.joinToString("\n") { "• ${it.detail}" }
        warnings.isNotEmpty() ->
            "Ready, with warnings:\n" + warnings.joinToString("\n") { "• ${it.detail}" }
        else -> "All checks passed."
    }
}

/**
 * End-to-end launch readiness for an instance.
 *
 * Every condition the launch path needs is checked here, in the order the launch hits it,
 * so the UI can name the first thing that is wrong instead of swallowing an exception.
 */
object LaunchPreflight {

    fun inspect(profileId: String): LaunchReport {
        val profile = Store_lookupProfile(profileId)
            ?: return LaunchReport(
                checks = listOf(LaunchCheck("Profile", false, "That profile no longer exists.")),
                versionId = "", profileId = profileId, profileName = "?",
            )

        val versionId = profile.versionId.trim()
        val checks = mutableListOf<LaunchCheck>()

        // 1. A version must be bound to the profile.
        if (versionId.isBlank()) {
            checks += LaunchCheck(
                "Version selected", false,
                "No Minecraft version is selected for \"${profile.name}\". " +
                    "Open Versions, install one, and tap Use.",
            )
            return LaunchReport(checks, versionId, profileId, profile.name)
        }

        // 2. The version json must exist on disk.
        val json = OrbitPaths.versionJson(versionId)
        if (!json.isFile) {
            checks += LaunchCheck(
                "Version installed", false,
                "\"$versionId\" is not installed (no ${json.name} in " +
                    "${OrbitPaths.versionDir(versionId).absolutePath}). Install it from Versions.",
            )
            return LaunchReport(checks, versionId, profileId, profile.name)
        }
        checks += LaunchCheck("Version installed", true, "$versionId present")

        // 3. A loader profile needs its vanilla parent.
        val parsed = runCatching { VersionJson.parse(versionId, JSONObject(json.readText())) }
            .getOrElse {
                checks += LaunchCheck(
                    "Version json readable", false,
                    "The version json for $versionId is corrupt: ${it.message}. Reinstall it.",
                )
                return LaunchReport(checks, versionId, profileId, profile.name)
            }
        parsed.inheritsFrom?.let { parent ->
            if (OrbitPaths.versionJson(parent).isFile) {
                checks += LaunchCheck("Parent version", true, "inherits from $parent")
            } else {
                checks += LaunchCheck(
                    "Parent version", false,
                    "\"$versionId\" is a mod-loader profile but its base version " +
                        "\"$parent\" is missing. Install vanilla $parent, then reinstall the loader.",
                )
                return LaunchReport(checks, versionId, profileId, profile.name)
            }
        }

        // 4. The client jar the launcher passes on the classpath.
        val jar = OrbitPaths.versionJar(versionId)
        if (jar.isFile && jar.length() > 0) {
            checks += LaunchCheck("Client jar", true, "${jar.name} (${jar.length() / 1_048_576} MB)")
        } else {
            val loader = LoaderApi.describe(versionId)
            checks += LaunchCheck(
                "Client jar", false,
                if (loader == "Vanilla") "Client jar missing — reinstall $versionId from Versions."
                else "$loader jar missing — reinstall $versionId from Versions.",
            )
        }

        // 5. At least one library must resolve, or the JVM has no classpath.
        val classpath = runCatching { MojangApi.classpathFor(MojangApi.resolveWithParents(versionId)) }
            .getOrElse { emptyList() }
        if (classpath.isEmpty()) {
            checks += LaunchCheck(
                "Libraries", false,
                "No libraries are present for $versionId. Reinstall it from Versions.",
            )
        } else {
            checks += LaunchCheck("Libraries", true, "${classpath.size} jars on the classpath")
        }

        // 6. Runtime: downloaded on demand, so absence is a warning, not a blocker.
        val javaMajor = RuntimeCatalog.requiredJavaMajor(parsed.javaVersionMajor, versionId.substringBefore('-'))
        val runtimeHome = runCatching {
            RuntimeCatalog.primaryAbi().let { abi ->
                RuntimeExtractor.findHome(OrbitPaths.runtimeDir("jre$javaMajor-$abi"))
            }
        }.getOrNull()
        checks += LaunchCheck(
            "Java runtime", runtimeHome != null,
            if (runtimeHome != null) "Java $javaMajor ready"
            else "Java $javaMajor runtime not installed yet — it downloads on first launch (~40 MB).",
            blocking = false,
        )

        // 7. The native GL bridge: absence does not stop the JVM, but nothing renders.
        checks += LaunchCheck(
            "Native GL bridge", NativeBridge.available,
            if (NativeBridge.available) "EGL bridge present"
            else NativeBridge.missingHint,
            blocking = false,
        )

        return LaunchReport(checks, versionId, profileId, profile.name)
    }

    /** Resolve a profile without importing the whole Store surface into this file. */
    private fun Store_lookupProfile(id: String) = com.orbitx.launcher.data.Store.lookupProfile(id)
}

/** Log file for a version's most recent session, so a failure is inspectable afterwards. */
fun sessionLogFile(versionId: String): File = GameLauncher.logFile(versionId.ifBlank { "unknown" })
