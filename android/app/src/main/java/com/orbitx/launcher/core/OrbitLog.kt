package com.orbitx.launcher.core

import android.util.Log

/**
 * Small append-only log sink so a failed session can be inspected after the fact
 * (gameDir/logs/orbitx.log), mirroring PojavLauncher's launcher log behaviour.
 */
object OrbitLog {
    private const val TAG = "OrbitX"
    private val buffer = StringBuilder()
    private var file: java.io.File? = null

    fun attach(logFile: java.io.File) { file = logFile }

    fun i(msg: String) = write("I", msg)
    fun w(msg: String) = write("W", msg)
    fun e(msg: String, t: Throwable? = null) {
        write("E", msg + (t?.let { ": ${it::class.java.simpleName}: ${it.message}" } ?: ""))
        t?.stackTrace?.forEach { write("E", "    at $it") }
    }

    fun tail(): String = synchronized(buffer) { buffer.toString() }

    private fun write(level: String, msg: String) {
        val line = "[$level] $msg"
        when (level) {
            "E" -> Log.e(TAG, msg)
            "W" -> Log.w(TAG, msg)
            else -> Log.i(TAG, msg)
        }
        synchronized(buffer) {
            buffer.append(line).append('\n')
            if (buffer.length > 512 * 1024) buffer.delete(0, buffer.length - 384 * 1024)
        }
        runCatching {
            file?.let { f ->
                if (!f.parentFile.exists()) f.parentFile.mkdirs()
                f.appendText(line + "\n")
            }
        }
    }
}
