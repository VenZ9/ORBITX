package com.orbitx.launcher.core

import java.io.File

/**
 * Assembles and starts a Minecraft: Java Edition session on Android.
 *
 * Launch strategy: spawn the provisioned runtime's own `java` executable with the
 * argument vector the official launcher would use, and hand the GL translation layer
 * its configuration through the environment. This is the classic ProcessBuilder path
 * used by the v2-generation PojavLauncher/Boardwalk lineage; it keeps the JVM in its
 * own process, so a crashed game cannot take the launcher down with it and the exit
 * code is observable.
 *
 * Required for a real session (and deliberately not faked here):
 *  - an Android-patched OpenJDK runtime          (provisioned on first launch)
 *  - an Android build of LWJGL/GLFW              (provisioned as a version library)
 *  - a GL translation layer .so                  (GL4ES / ANGLE / Mesa-Zink)
 */
object GameLauncher {

    data class Result(val exitCode: Int, val log: String)

    /** Where the JVM's stdout/stderr is captured. */
    fun logFile(versionId: String) = File(OrbitPaths.logsDir, "game-$versionId.log")

    /**
     * Provision everything the session needs and return the process. The caller owns
     * [onLine] streaming and process lifecycle.
     */
    fun prepareAndLaunch(
        versionId: String,
        session: SessionInfo,
        settings: LaunchSettings,
        onStage: (String) -> Unit,
        onProgress: ((Progress) -> Unit)? = null,
        onLine: (String) -> Unit,
    ): Result {
        val renderer = OrbitRenderer.fromId(settings.rendererId)
        onStage("Resolving version $versionId")

        if (!OrbitPaths.versionJson(versionId).isFile) {
            throw IllegalStateException("version $versionId is not installed")
        }
        val v = MojangApi.resolveWithParents(versionId)

        // 1. Runtime ----------------------------------------------------------------
        onStage("Checking Java runtime")
        val javaMajor = settings.javaMajorOverride
            ?: RuntimeCatalog.requiredJavaMajor(v.javaVersionMajor, versionId.substringBefore('-'))
        val runtime = provisionRuntime(javaMajor, onStage, onLine)
        val javaExe = File(runtime, "bin/java")
        if (!javaExe.isFile) throw IllegalStateException("runtime at $runtime has no bin/java")

        // 2. Game files -------------------------------------------------------------
        onStage("Verifying game files")
        MojangApi.installClientJar(v)
        MojangApi.installLibraries(v, onProgress)
        if (!settings.skipAssets) MojangApi.installAssets(v, onProgress)
        val natives = MojangApi.installNatives(v)

        // 3. Assemble the command line ---------------------------------------------
        onStage("Building launch command")
        val classpath = ArrayList<File>()
        classpath += MojangApi.classpathFor(v)
        val clientJar = OrbitPaths.versionJar(v.id)
        if (clientJar.isFile) classpath += clientJar
        if (classpath.isEmpty()) throw IllegalStateException("no classpath entries resolved")

        val gameDir = OrbitPaths.instanceDir(settings.instanceId ?: versionId)
        val env = LaunchEnvironment(
            javaHome = runtime,
            nativesDir = natives,
            gameDir = gameDir,
            assetsDir = OrbitPaths.assetsDir,
            assetsIndexId = v.assetsIndexId ?: "legacy",
            versionId = versionId,
            versionType = v.type.ifBlank { "release" },
            classpath = classpath,
            ramMb = settings.ramMb,
            width = settings.width,
            height = settings.height,
        )

        val argv = ArrayList<String>()
        argv += javaExe.absolutePath
        argv += ArgumentBuilder.buildJvmArgs(v, env, session, renderer)
        argv += v.mainClass
        argv += ArgumentBuilder.buildGameArgs(v, env, session)

        // 4. Environment ------------------------------------------------------------
        onStage("Starting JVM")
        val pb = ProcessBuilder(argv)
        pb.directory(gameDir)
        pb.redirectErrorStream(true)
        val e = pb.environment()

        e["JAVA_HOME"] = runtime.absolutePath
        e["HOME"] = gameDir.absolutePath
        e["TMPDIR"] = OrbitPaths.cacheDir.absolutePath
        e["PATH"] = "${File(runtime, "bin").absolutePath}:" + (System.getenv("PATH") ?: "/system/bin")

        // Native search path: the runtime's own libs first (libjvm lives there),
        // then the extracted LWJGL natives, then any driver libs.
        val libPath = listOf(
            File(runtime, "lib").absolutePath,
            File(runtime, "lib/server").absolutePath,
            natives.absolutePath,
            File(runtime, "lib/jli").absolutePath,
            System.getenv("LD_LIBRARY_PATH") ?: "/system/lib64:/vendor/lib64",
        ).joinToString(":")
        e["LD_LIBRARY_PATH"] = libPath
        e["POJAV_NATIVEDIR"] = natives.absolutePath

        // Renderer contract, exactly as upstream expects it.
        e["POJAV_RENDERER"] = renderer.poJavName
        e["LIBGL_ES"] = renderer.glesMajor.toString()
        e["LIBGL_MIPMAP"] = "3"
        e["LIBGL_NORMALIZE"] = "1"
        e["LIBGL_NOERROR"] = "0"
        e["MESA_GLSL_CACHE_DIR"] = OrbitPaths.cacheDir.absolutePath
        e["AWTSTUB_WIDTH"] = settings.width.toString()
        e["AWTSTUB_HEIGHT"] = settings.height.toString()
        renderer.extraEnv.forEach { (k, value) ->
            e[k] = if (k == "VTEST_SOCKET_NAME") File(OrbitPaths.cacheDir, value).absolutePath else value
        }
        settings.customEnv.forEach { (k, value) -> e[k] = value }

        OrbitLog.i("launching: ${argv.joinToString(" ")}")
        OrbitLog.i("renderer: ${renderer.title} (POJAV_RENDERER=${renderer.poJavName})")

        val proc = pb.start()
        val log = StringBuilder()
        proc.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                synchronized(log) { log.append(line).append('\n') }
                OrbitLog.i("game: $line")
                onLine(line)
            }
        }
        val code = proc.waitFor()
        return Result(code, log.toString())
    }

    /**
     * Ensure a JRE of [javaMajor] exists locally, downloading a PojavLauncher OpenJDK
     * build on first use. Returns the runtime home.
     */
    fun provisionRuntime(javaMajor: Int, onStage: (String) -> Unit, onLine: (String) -> Unit): File {
        val abi = RuntimeCatalog.primaryAbi()
        val name = "jre$javaMajor-$abi"
        val home = OrbitPaths.runtimeDir(name)

        RuntimeExtractor.findHome(home)?.let { existing ->
            OrbitLog.i("runtime $name already installed at $existing")
            return existing
        }

        onStage("Downloading Java $javaMajor runtime ($abi)")
        val candidates = RuntimeCatalog.knownRuntimeUrls(javaMajor, abi)
        if (candidates.isEmpty()) {
            throw IllegalStateException(
                "No Java $javaMajor runtime is published for $abi. Install a runtime " +
                    "manually into ${OrbitPaths.runtimesDir.absolutePath}/$name."
            )
        }

        var lastErr: Exception? = null
        for (url in candidates) {
            val fileName = url.substringAfterLast('/')
            val archive = File(OrbitPaths.cacheDir, fileName)
            try {
                onStage("Downloading $fileName")
                Downloader.fetch(url, archive)
                onStage("Extracting runtime $name")
                val found = RuntimeExtractor.extract(
                    archive, home,
                    tarXz = fileName.endsWith(".tar.xz") || fileName.endsWith(".tar"),
                    onLog = onLine,
                )
                // The binary must be executable; some tars lose the mode bit.
                File(found, "bin/java").setExecutable(true, false)
                archive.delete()
                OrbitLog.i("runtime $name ready at $found")
                return found
            } catch (err: Exception) {
                lastErr = err
                OrbitLog.w("runtime download failed from $url: ${err.message}")
            }
        }
        throw lastErr ?: IllegalStateException("runtime provisioning failed for Java $javaMajor")
    }
}

/** Everything the launcher's Settings screen controls, passed into a launch. */
data class LaunchSettings(
    val ramMb: Int = 2048,
    val rendererId: String = OrbitRenderer.GL4ES_ES3.id,
    val width: Int = 1280,
    val height: Int = 720,
    val javaMajorOverride: Int? = null,
    val instanceId: String? = null,
    val skipAssets: Boolean = false,
    val customEnv: Map<String, String> = emptyMap(),
)
