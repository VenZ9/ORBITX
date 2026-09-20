package com.orbitx.launcher.core

import android.os.Build
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * GL translation layers.
 *
 * Minecraft: Java Edition speaks desktop OpenGL; Android only offers GLES (and
 * Vulkan). Every working Java launcher therefore inserts a translation layer, and
 * which layer you pick is the single biggest performance/compatibility decision.
 *
 * The names and the environment variables are PojavLauncher's contract verbatim, so
 * the bundled LWJGL Android build and the translation libraries select the same
 * codepath they do upstream:
 *   POJAV_RENDERER      -> which bridge to load
 *   LIBGL_ES            -> requested GLES major version
 *   POJAVEXEC_EGL       -> override the EGL implementation (used by ANGLE)
 *   MESA_LOADER_DRIVER_OVERRIDE -> Mesa driver (zink)
 *   -Dorg.lwjgl.opengl.libname  -> the .so LWJGL should dlopen
 */
enum class OrbitRenderer(
    val id: String,
    val title: String,
    /** Value written to POJAV_RENDERER (PojavLauncher-compatible). */
    val poJavName: String,
    /** Native library LWJGL is told to load. */
    val lwjglLibName: String,
    val glesMajor: Int,
    val description: String,
    /** Extra environment entries this renderer needs. */
    val extraEnv: Map<String, String> = emptyMap(),
) {
    GL4ES_ES2(
        id = "gl4es_es2",
        title = "GL4ES (OpenGL ES 2)",
        poJavName = "opengles2",
        lwjglLibName = "libgl4es_114.so",
        glesMajor = 2,
        description = "Most compatible. Widest device support, lowest feature level.",
    ),
    GL4ES_ES25(
        id = "gl4es_es25",
        title = "GL4ES (OpenGL ES 2.5)",
        poJavName = "opengles2_5",
        lwjglLibName = "libgl4es_114.so",
        glesMajor = 3,
        description = "GLES 2 surface with extra extensions exposed.",
    ),
    GL4ES_ES3(
        id = "gl4es_es3",
        title = "GL4ES (OpenGL ES 3)",
        poJavName = "opengles3",
        lwjglLibName = "libgl4es_114.so",
        glesMajor = 3,
        description = "Best all-round choice for modern versions on GLES 3 hardware.",
    ),
    VIRGL(
        id = "virgl",
        title = "VirGL",
        poJavName = "opengles3",
        lwjglLibName = "libgl4es_114.so",
        glesMajor = 3,
        description = "Virtualised GL passthrough for emulated/host-GL setups. Slower but very faithful.",
        extraEnv = mapOf(
            "VTEST_SOCKET_NAME" to ".virgl_test",
            "MESA_GL_VERSION_OVERRIDE" to "4.3",
        ),
    ),
    ZINK(
        id = "zink",
        title = "Zink (OpenGL -> Vulkan)",
        poJavName = "vulkan_zink",
        lwjglLibName = "libOSMesa.so",
        glesMajor = 3,
        description = "Mesa Zink, translating GL to Vulkan. Best on devices with mature Vulkan drivers.",
        extraEnv = mapOf(
            "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
        ),
    ),
    ZINK_TURNIP(
        id = "zink_turnip",
        title = "Zink + Turnip (Adreno)",
        poJavName = "vulkan_zink",
        lwjglLibName = "libOSMesa.so",
        glesMajor = 3,
        description = "Zink on the bundled Turnip Vulkan driver. Adreno GPUs only.",
        extraEnv = mapOf(
            "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
            "POJAV_LOAD_TURNIP" to "1",
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
        ),
    ),
    ANGLE(
        id = "angle",
        title = "ANGLE",
        poJavName = "opengles3_ltw",
        lwjglLibName = "libltw.so",
        glesMajor = 3,
        description = "ANGLE's EGL layered over Vulkan. Good on drivers where GLES is broken.",
        extraEnv = mapOf("POJAVEXEC_EGL" to "libltw.so"),
    );

    companion object {
        fun fromId(id: String?): OrbitRenderer =
            entries.firstOrNull { it.id == id } ?: GL4ES_ES3
    }
}

/**
 * Per-architecture runtime descriptors.
 *
 * The JREs are the PojavLauncher open-source OpenJDK builds
 * (PojavLauncherTeam/android-openjdk-build-multiarch), which are the only OpenJDK
 * builds patched for Android's Bionic libc and are what every Java-on-Android
 * launcher uses. They are downloaded on first use rather than bundled, because a
 * single JRE is ~60-120 MB and there is one per (Java version, ABI).
 */
data class RuntimeDescriptor(
    val name: String,       // "jre8", "jre17", "jre21"
    val javaMajor: Int,
    val abi: String,        // arm64-v8a, armeabi-v7a, x86_64
    val url: String,
    val sha1: String? = null,
    /** true when the archive is a .tar.xz, false for .zip */
    val tarXz: Boolean = true,
    val relativeHome: String = "",
)

object RuntimeCatalog {
    private const val BASE =
        "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download"

    /** Java major version required by a Minecraft version. */
    fun requiredJavaMajor(versionJsonJava: Int?, mcId: String): Int {
        versionJsonJava?.let { return it }
        // Fallback heuristic for old/odd jsons.
        val v = mcId.substringBefore('-').let { parseMinor(it) }
        return when {
            v == null -> 17
            v.first > 1 || (v.first == 1 && v.second >= 20) -> 21
            v.first == 1 && v.second >= 18 -> 17
            v.first == 1 && v.second >= 17 -> 16
            else -> 8
        }
    }

    private fun parseMinor(id: String): Pair<Int, Int>? {
        val m = Regex("^(\\d+)(?:\\.(\\d+))?").find(id.trim()) ?: return null
        val a = m.groupValues[1].toIntOrNull() ?: return null
        val b = m.groupValues[2].toIntOrNull() ?: 0
        return a to b
    }

    /**
     * Published runtime archives, keyed by Java major version.
     *
     * Source: AngelAuraMC/angelauramc-openjdk-build — the maintained successor of
     * PojavLauncherTeam/android-openjdk-build-multiarch (which is archived). These are
     * OpenJDK builds patched for Android/Bionic; the desktop JDK does not run here.
     */
    private const val RUNTIME_BASE =
        "https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download"

    /**
     * Candidate download URLs for [javaMajor] on [abi], newest-first.
     * Returns an empty list when that (version, ABI) combination was never published,
     * so the caller reports a clear error instead of attempting a broken launch.
     */
    fun knownRuntimeUrls(javaMajor: Int, abi: String): List<String> {
        val fileAbi = fileAbiName(abi) ?: return emptyList()
        val releases = when (javaMajor) {
            8 -> listOf("download_jre8", "download")
            11 -> listOf("download_jre17", "download")
            16, 17 -> listOf("download_jre17", "download")
            21 -> listOf("download_jre21", "download")
            25 -> listOf("download_jre25", "download")
            else -> listOf("download_jre21", "download")
        }
        val tag = if (javaMajor <= 8) "jre8" else "jre$javaMajor"
        return releases
            .map { "$RUNTIME_BASE/$it/$tag-android-$fileAbi.tar.xz" }
            .distinct()
    }

    fun primaryAbi(): String {
        val abis = Build.SUPPORTED_ABIS
        return abis.firstOrNull { it in SUPPORTED } ?: "arm64-v8a"
    }

    /** Android ABI name -> the token used in the published runtime file names. */
    fun fileAbiName(abi: String): String? = when (abi) {
        "arm64-v8a" -> "arm64"
        "armeabi-v7a" -> "arm"
        "x86_64" -> "x86_64"
        "x86" -> "x86"
        else -> null
    }

    val SUPPORTED = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
}

/** Unpacking of a downloaded runtime archive into OrbitPaths.runtimesDir. */
object RuntimeExtractor {
    private const val TAG = "RuntimeExtractor"

    /** Returns the runtime home (the directory containing bin/java). */
    fun extract(archive: File, destDir: File, tarXz: Boolean, onLog: (String) -> Unit): File {
        destDir.mkdirs()
        val isXz = archive.name.endsWith(".xz")
        if (tarXz || isXz) {
            extractTar(archive, destDir, isXz, onLog)
        } else {
            extractZip(archive, destDir, onLog)
        }
        findHome(destDir)?.let { return it }
        throw IllegalStateException("no bin/java found after extracting ${archive.name}")
    }

    private fun extractTar(archive: File, destDir: File, xz: Boolean, onLog: (String) -> Unit) {
        val raw = BufferedInputStream(archive.inputStream(), 64 * 1024)
        val stream = if (xz) XZInputStream(raw) else raw
        TarArchiveInputStream(stream).use { tar ->
            var e = tar.nextEntry
            while (e != null) {
                val clean = e.name.trimStart('/', '.').removePrefix("../")
                if (clean.isNotBlank() && !clean.contains("..")) {
                    val out = File(destDir, clean)
                    if (e.isDirectory) out.mkdirs()
                    else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { fos -> tar.copyTo(fos) }
                        if (out.name == "java" || out.path.contains("/bin/")) out.setExecutable(true, false)
                        if (out.path.contains("/lib/")) out.setReadable(true, false)
                    }
                }
                e = tar.nextEntry
            }
        }
        onLog("extracted tar archive ${archive.name}")
    }

    private fun extractZip(archive: File, destDir: File, onLog: (String) -> Unit) {
        java.util.zip.ZipInputStream(archive.inputStream().buffered()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                val clean = e.name.trimStart('/', '.')
                if (clean.isNotBlank() && !clean.contains("..")) {
                    val out = File(destDir, clean)
                    if (e.isDirectory) out.mkdirs()
                    else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                        if (out.name == "java") out.setExecutable(true, false)
                    }
                }
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        onLog("extracted zip archive ${archive.name}")
    }

    /** The runtime home is the ancestor of the first executable bin/java found. */
    fun findHome(root: File): File? {
        val stack = ArrayDeque<File>().apply { add(root) }
        var depth = 0
        while (stack.isNotEmpty() && depth < 50_000) {
            val dir = stack.removeFirst(); depth++
            val javaBin = File(dir, "bin/java")
            if (javaBin.isFile) return dir
            dir.listFiles()?.forEach { if (it.isDirectory) stack.add(it) }
        }
        return null
    }
}
