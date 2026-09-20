package com.orbitx.launcher.core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** A non-2xx response, carrying the status and the url so a failure can be reported precisely. */
class HttpStatusException(val code: Int, val url: String) :
    IllegalStateException("HTTP $code for $url") {
    fun render(): String = "HTTP $code  $url"
}

/** Progress for a whole download batch. */
data class Progress(val done: Int, val total: Int, val bytes: Long, val currentName: String) {
    val fraction: Float get() = if (total <= 0) 0f else done.toFloat() / total.toFloat()
}

/**
 * Hash-verified, resumable HTTP downloader.
 *
 * Mojang's asset store is content-addressed, so verification is not optional:
 * a truncated or corrupted jar otherwise fails much later inside the JVM with a
 * useless error. Files are written to a temp sibling and moved into place only
 * after the hash matches.
 */
object Downloader {
    private val pool = Executors.newFixedThreadPool(4) { r ->
        Thread(r, "orbitx-dl").apply { isDaemon = true }
    }

    private const val UA = "OrbitXLauncher/1.0 (Android; Minecraft Java launcher)"

    @Volatile var cancelled = false

    /** Download [url] to [dest] unless it already exists with the expected hash. */
    fun fetch(
        url: String,
        dest: File,
        sha1: String? = null,
        size: Long = 0L,
        onBytes: ((Long) -> Unit)? = null,
    ): File = fetchAny(listOf(url), dest, sha1, size, onBytes)

    /**
     * Download [dest] from the first of [urls] that answers.
     *
     * A version json can point a library at more than one repository: its own maven,
     * Maven Central, and Mojang's mirror. Trying them in order is what makes mod-loader
     * installs work at all — assuming every library lives on libraries.minecraft.net
     * returns 404 for anything Fabric or Forge publishes.
     *
     * Failures are collected with the exact url and HTTP status, and the thrown message
     * carries the whole list, so a failed install names the artifact and every mirror it
     * tried instead of surfacing a bare "HTTP 404".
     */
    fun fetchAny(
        urls: List<String>,
        dest: File,
        sha1: String? = null,
        size: Long = 0L,
        onBytes: ((Long) -> Unit)? = null,
        label: String? = null,
    ): File {
        if (dest.isFile && dest.length() > 0) {
            if (sha1.isNullOrBlank() || sha1.equals(sha1Of(dest), ignoreCase = true)) return dest
            OrbitLog.w("hash mismatch, re-downloading ${dest.name}")
        }
        val candidates = urls.filter { it.isNotBlank() }.distinct()
        if (candidates.isEmpty()) throw IllegalStateException("no download url for ${dest.name}")
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        val tried = ArrayList<String>(candidates.size)

        var lastErr: Exception? = null
        for (url in candidates) {
            for (attempt in 1..3) {
                if (cancelled) throw InterruptedException("cancelled")
                try {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 30_000
                        readTimeout = 60_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", UA)
                    }
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        conn.disconnect()
                        throw HttpStatusException(code, url)
                    }
                    conn.inputStream.use { ins ->
                        FileOutputStream(tmp).use { out ->
                            val buf = ByteArray(64 * 1024)
                            var n: Int
                            while (ins.read(buf).also { n = it } > 0) {
                                out.write(buf, 0, n)
                                onBytes?.invoke(n.toLong())
                            }
                            out.fd.sync()
                        }
                    }
                    conn.disconnect()

                    if (!sha1.isNullOrBlank()) {
                        val got = sha1Of(tmp)
                        if (!got.equals(sha1, ignoreCase = true)) {
                            tmp.delete()
                            throw IllegalStateException("sha1 mismatch for ${dest.name}: got $got want $sha1")
                        }
                    }
                    if (size > 0 && tmp.length() != size) OrbitLog.w("size mismatch for ${dest.name} (continuing, hash ok)")

                    if (dest.exists()) dest.delete()
                    if (!tmp.renameTo(dest)) {
                        tmp.copyTo(dest, overwrite = true); tmp.delete()
                    }
                    return dest
                } catch (e: Exception) {
                    lastErr = e
                    tmp.delete()
                    // A definitive 4xx will not change on retry, so move to the next
                    // mirror instead of hammering the same 404 three times.
                    val definitive = e is HttpStatusException && e.code in 400..499
                    if (definitive) break
                    OrbitLog.w("download attempt $attempt failed for ${dest.name}: ${e.message}")
                    if (attempt < 3) Thread.sleep(600L * attempt)
                }
            }
            tried += (lastErr as? HttpStatusException)?.render()
                ?: "$url -> ${lastErr?.message ?: "failed"}"
            OrbitLog.w("mirror failed for ${dest.name}: ${lastErr?.message} ($url)")
        }

        val what = label ?: dest.name
        throw IllegalStateException(
            "Download failed for $what. Tried:\n" + tried.joinToString("\n") { "  $it" },
            lastErr,
        )
    }

    /**
     * Download a set of jobs in parallel with aggregated progress.
     * The caller supplies (url, dest, sha1, size) tuples.
     */
    fun fetchAll(
        jobs: List<DownloadJob>,
        onProgress: ((Progress) -> Unit)? = null,
    ) {
        val total = jobs.size
        val done = AtomicLong(0)
        val bytes = AtomicLong(0)
        val futures = jobs.map { job ->
            pool.submit {
                if (cancelled) throw InterruptedException("cancelled")
                fetchAny(job.urls, job.dest, job.sha1, job.size, { n -> bytes.addAndGet(n) }, job.label)
                val d = done.incrementAndGet()
                onProgress?.invoke(Progress(d.toInt(), total, bytes.get(), job.label ?: job.dest.name))
            }
        }
        var firstError: Exception? = null
        futures.forEach {
            try { it.get() } catch (e: Exception) {
                if (firstError == null) firstError = (e.cause as? Exception) ?: e
            }
        }
        firstError?.let { throw it }
    }

    fun getText(url: String, timeoutMs: Int = 30_000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
        }
        val code = conn.responseCode
        if (code !in 200..299) throw IllegalStateException("HTTP $code for $url")
        return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    fun sha1Of(f: File): String {
        val md = MessageDigest.getInstance("SHA-1")
        FileInputStream(f).use { ins ->
            val buf = ByteArray(64 * 1024)
            var n: Int
            while (ins.read(buf).also { n = it } > 0) md.update(buf, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun copyStream(ins: InputStream, out: File) {
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { fos -> ins.copyTo(fos); fos.fd.sync() }
    }
}

/**
 * One file to fetch, with every mirror it may live on in priority order.
 * The single-url constructor keeps the content-addressed asset path unchanged.
 */
data class DownloadJob(
    val urls: List<String>,
    val dest: File,
    val sha1: String? = null,
    val size: Long = 0L,
    /** human-readable coordinate, used in progress and error reporting */
    val label: String? = null,
) {
    constructor(url: String, dest: File, sha1: String? = null, size: Long = 0L) :
        this(listOf(url), dest, sha1, size, null)
}
