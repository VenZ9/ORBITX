package com.orbitx.launcher

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.orbitx.launcher.core.GameLauncher
import com.orbitx.launcher.core.LaunchSettings
import com.orbitx.launcher.core.OrbitLog
import com.orbitx.launcher.core.SessionInfo
import com.orbitx.launcher.data.Store
import com.orbitx.launcher.ui.OrbitXTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hosts a running Minecraft session.
 *
 * The GL surface is a SurfaceView whose Surface is handed to the native EGL bridge
 * (orbitx_egl_bridge.c) when the native library is present; the JVM itself runs as a
 * child process so a game crash cannot take the launcher down with it.
 */
class GameActivity : ComponentActivity() {

    private val output = mutableStateListOf<String>()
    private var stage by mutableStateOf("Starting…")
    private var exitCode by mutableStateOf<Int?>(null)

    /** Implemented in cpp/orbitx_egl_bridge.c; only present in -Porbitx.native builds. */
    private external fun nativeEglSetup(surface: android.view.Surface): Boolean
    private external fun nativeEglResize(w: Int, h: Int)
    private external fun nativeEglTeardown()

    private val nativeAvailable: Boolean by lazy {
        runCatching { System.loadLibrary("orbitx_bridge"); true }.getOrElse { false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = Store.currentProfile()
        val versionId = profile?.versionId.orEmpty()

        setContent {
            OrbitXTheme {
                GameSurface(
                    output = output,
                    stage = stage,
                    exitCode = exitCode,
                    nativeAvailable = nativeAvailable,
                    onSurfaceReady = { surface, w, h ->
                        if (nativeAvailable && surface != null) {
                            runCatching { nativeEglSetup(surface) }
                                .onFailure { OrbitLog.w("EGL setup failed: ${it.message}") }
                        }
                    },
                    onSurfaceResized = { w, h ->
                        if (nativeAvailable) runCatching { nativeEglResize(w, h) }
                    },
                )
            }
        }

        if (profile == null || versionId.isBlank()) {
            append("[error] no profile with a selected Minecraft version")
            stage = "Nothing to launch"
            return
        }

        val session = SessionInfo(
            playerName = profile.name,
            uuid = profile.uuid,
            accessToken = "0",           // offline sessions do not authenticate
        )
        val settings = LaunchSettings(
            ramMb = profile.ramMb,
            rendererId = profile.rendererId,
            width = profile.width,
            height = profile.height,
            javaMajorOverride = profile.javaMajor.takeIf { it > 0 },
            skipAssets = Store.state.value.settings.skipAssets,
        )

        Thread {
            try {
                val result = GameLauncher.prepareAndLaunch(
                    versionId = versionId,
                    session = session,
                    settings = settings,
                    onStage = { s -> runOnUiThread { stage = s } },
                    onProgress = { p ->
                        if (p.total > 0) runOnUiThread {
                            stage = "${p.currentName}  (${p.done}/${p.total})"
                        }
                    },
                    onLine = { line -> runOnUiThread { append(line) } },
                )
                runOnUiThread {
                    stage = "Exited with code ${result.exitCode}"
                    exitCode = result.exitCode
                }
            } catch (t: Throwable) {
                OrbitLog.e("launch failed", t)
                runOnUiThread {
                    stage = "Launch failed"
                    append("[error] ${t::class.java.simpleName}: ${t.message}")
                    append("[hint] see ${File(com.orbitx.launcher.core.OrbitPaths.logsDir, "orbitx.log")}")
                }
            }
        }.apply { name = "orbitx-session"; isDaemon = true }.start()
    }

    private fun append(line: String) {
        if (output.size > 2000) output.removeAt(0)
        output.add(line)
    }

    override fun onDestroy() {
        if (nativeAvailable) runCatching { nativeEglTeardown() }
        super.onDestroy()
    }
}

@Composable
private fun GameSurface(
    output: List<String>,
    stage: String,
    exitCode: Int?,
    nativeAvailable: Boolean,
    onSurfaceReady: (android.view.Surface?, Int, Int) -> Unit,
    onSurfaceResized: (Int, Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color(0xFF07080C))) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (nativeAvailable) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(h: SurfaceHolder) {
                                    onSurfaceReady(h.surface, width, height)
                                }
                                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
                                    onSurfaceResized(w, hh)
                                }
                                override fun surfaceDestroyed(h: SurfaceHolder) = Unit
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "GL surface unavailable\n\nThe native EGL bridge is not compiled into " +
                            "this build (no NDK / -Porbitx.native).\nThe JVM output below still " +
                            "reports provisioning and launch progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB9C0D4),
                    )
                }
            }
        }
        Text(
            text = if (exitCode != null) "$stage" else stage,
            color = Color(0xFF3DDC97),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().weight(0.9f).background(Color(0xFF0C0E14))) {
            items(output) { line ->
                Text(
                    line,
                    color = Color(0xFF8E96AC),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                )
            }
        }
    }
}
