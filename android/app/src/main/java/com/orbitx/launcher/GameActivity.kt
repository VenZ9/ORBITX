package com.orbitx.launcher

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.orbitx.launcher.core.GameLauncher
import com.orbitx.launcher.core.LaunchSettings
import com.orbitx.launcher.core.NativeBridge
import com.orbitx.launcher.core.OrbitLog
import com.orbitx.launcher.core.OrbitPaths
import com.orbitx.launcher.core.RuntimeCatalog
import com.orbitx.launcher.core.RuntimeExtractor
import com.orbitx.launcher.core.SessionInfo
import com.orbitx.launcher.data.Store
import com.orbitx.launcher.ui.OrbitXTheme
import java.io.File

/**
 * Hosts a running Minecraft session.
 *
 * The GL surface is a SurfaceView whose Surface is handed to the native EGL bridge
 * (orbitx_egl_bridge.c) when that library is present in the APK; the JVM itself runs as a
 * child process so a game crash cannot take the launcher down with it.
 *
 * The launch is driven entirely by the extras [EXTRA_PROFILE_ID] / [EXTRA_VERSION_ID] so
 * the session always belongs to the profile whose Play button was pressed, rather than to
 * whatever is selected by the time a background thread reads the store.
 *
 * Every stage is logged (logcat tag `OrbitX` and `orbitx/logs/orbitx.log`), and every
 * condition that would prevent a session is reported on screen, so a failure is never
 * silent and never needs guesswork to locate.
 */
class GameActivity : ComponentActivity() {

    private val output = mutableStateListOf<String>()
    private var stage by mutableStateOf("Starting…")
    private var exitCode by mutableStateOf<Int?>(null)
    private var blocker by mutableStateOf<String?>(null)

    /** Implemented in cpp/orbitx_egl_bridge.c; only present in -Porbitx.native builds. */
    private external fun nativeEglSetup(surface: android.view.Surface): Boolean
    private external fun nativeEglResize(w: Int, h: Int)
    private external fun nativeEglTeardown()

    private val nativeAvailable: Boolean get() = NativeBridge.available

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profileId = intent?.getStringExtra(EXTRA_PROFILE_ID)
        val versionExtra = intent?.getStringExtra(EXTRA_VERSION_ID)
        // Fall back to the selected profile so a direct/manual start still behaves.
        val profile = Store.lookupProfile(profileId)
        val versionId = versionExtra?.takeIf { it.isNotBlank() }
            ?: profile?.versionId.orEmpty()

        append("[info] OrbitX session start")
        append("[info] profileId=$profileId versionId=$versionId")
        append("[info] native bridge=${if (nativeAvailable) "present" else "ABSENT"}")
        OrbitLog.i("GameActivity start: profile=$profileId version=$versionId native=$nativeAvailable")

        setContent {
            OrbitXTheme {
                GameSurface(
                    output = output,
                    stage = stage,
                    exitCode = exitCode,
                    blocker = blocker,
                    nativeAvailable = nativeAvailable,
                    onSurfaceReady = { surface, w, h ->
                        OrbitLog.i("GL surface ready ${w}x${h}")
                        if (nativeAvailable && surface != null) {
                            runCatching { nativeEglSetup(surface) }
                                .onFailure {
                                    OrbitLog.w("EGL setup failed: ${it.message}")
                                    runOnUiThread { blocker = "EGL setup failed: ${it.message}" }
                                }
                        }
                    },
                    onSurfaceResized = { w, h ->
                        if (nativeAvailable) runCatching { nativeEglResize(w, h) }
                    },
                )
            }
        }

        // --- Fail loudly and specifically, never silently -------------------------
        if (profile == null) {
            blocker = "No profile found for id=${profileId ?: "(none)"}. Open Profiles and " +
                "select one, then press Play again."
            append("[error] no profile resolved")
            stage = "Nothing to launch"
            return
        }
        if (versionId.isBlank()) {
            blocker = "Profile \"${profile.name}\" has no Minecraft version selected. Install " +
                "one on the Versions tab and tap Use."
            append("[error] profile has no version bound")
            stage = "Nothing to launch"
            return
        }
        if (!OrbitPaths.versionJson(versionId).isFile) {
            blocker = "\"$versionId\" is not installed (no version json). Install it from Versions."
            append("[error] version json missing for $versionId")
            stage = "Nothing to launch"
            return
        }
        if (!nativeAvailable) {
            // Not fatal: the JVM still provisions and logs. But say so on screen, because
            // without the bridge no rendered frame is possible and that has to be obvious.
            append("[warn] ${NativeBridge.missingHint}")
            blocker = NativeBridge.missingHint
        }

        val javaMajor = RuntimeCatalog.requiredJavaMajor(null, versionId.substringBefore('-'))
        append("[info] required Java major: $javaMajor")

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
                OrbitLog.i("provisioning + launching $versionId")
                val result = GameLauncher.prepareAndLaunch(
                    versionId = versionId,
                    session = session,
                    settings = settings,
                    onStage = { s ->
                        OrbitLog.i("stage: $s")
                        runOnUiThread { stage = s }
                    },
                    onProgress = { p ->
                        if (p.total > 0) runOnUiThread {
                            stage = "${p.currentName}  (${p.done}/${p.total})"
                        }
                    },
                    onLine = { line -> runOnUiThread { append(line) } },
                )
                OrbitLog.i("session exited with ${result.exitCode}")
                runOnUiThread {
                    stage = "Exited with code ${result.exitCode}"
                    exitCode = result.exitCode
                }
            } catch (t: Throwable) {
                OrbitLog.e("launch failed", t)
                val hint = diagnose(t)
                runOnUiThread {
                    stage = "Launch failed"
                    blocker = hint
                    append("[error] ${t::class.java.simpleName}: ${t.message}")
                    append("[hint] $hint")
                }
            }
        }.apply { name = "orbitx-session"; isDaemon = true }.start()
    }

    /**
     * Turn a launch exception into the actionable sentence the user needs.
     * The point is that a failure names its own cause instead of surfacing a stack trace.
     */
    private fun diagnose(t: Throwable): String {
        val msg = t.message.orEmpty()
        return when {
            msg.contains("Could not find or load main class") || msg.contains("NoClassDefFound") ->
                "The game's classpath is incomplete. Reinstall this version from the Versions tab."

            t is java.io.FileNotFoundException ||
                msg.contains("HTTP 404") || msg.contains("Download failed") ->
                "A required file could not be downloaded.\n$msg"

            t is java.io.IOException && msg.contains("No such file") ->
                "A runtime file is missing. Delete ${OrbitPaths.runtimesDir.absolutePath} and " +
                    "press Play again to re-provision Java ${
                        RuntimeCatalog.requiredJavaMajor(null, "1.21")
                    }."

            msg.contains("Permission denied") ->
                "The provisioned Java runtime is not executable. Delete " +
                    "${OrbitPaths.runtimesDir.absolutePath} and press Play again."

            msg.contains("no bin/java") ->
                "The Java runtime downloaded incompletely. Delete " +
                    "${OrbitPaths.runtimesDir.absolutePath} and press Play again."

            msg.contains("is not installed") ->
                "$msg"

            else -> "Launch failed: $msg\nSee ${File(OrbitPaths.logsDir, "orbitx.log")} for the " +
                "full log."
        }
    }

    private fun append(line: String) {
        if (output.size > 2000) output.removeAt(0)
        output.add(line)
    }

    override fun onDestroy() {
        OrbitLog.i("GameActivity destroy; native=$nativeAvailable")
        if (nativeAvailable) runCatching { nativeEglTeardown() }
        super.onDestroy()
    }

    companion object {
        /** The profile this session belongs to. */
        const val EXTRA_PROFILE_ID = "com.orbitx.launcher.PROFILE_ID"

        /** The version to launch, resolved by the Play button's preflight. */
        const val EXTRA_VERSION_ID = "com.orbitx.launcher.VERSION_ID"
    }
}

@Composable
private fun GameSurface(
    output: List<String>,
    stage: String,
    exitCode: Int?,
    blocker: String?,
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
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            "No rendered surface in this build",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFE7B84B),
                        )
                        Text(
                            "The native EGL bridge (orbitx_bridge.so) is not compiled into " +
                                "this APK. It is only built with an NDK toolchain " +
                                "(-Porbitx.native=true), so this build can provision files and " +
                                "run the JVM but cannot draw the game onto the screen.\n\n" +
                                "Everything the launcher does — runtime download, version and " +
                                "library resolution, the launch command — is shown below as it " +
                                "happens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB9C0D4),
                        )
                    }
                }
            }
        }

        blocker?.let { b ->
            Text(
                b,
                color = Color(0xFFE7B84B),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Text(
            text = stage,
            color = if (exitCode == null) Color(0xFF3DDC97) else Color(0xFF8E96AC),
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

/** Kept so the runtime-provisioning message can reference the concrete path. */
private val runtimeRootHint: File get() = OrbitPaths.runtimesDir

/** Unused-but-documented helper kept honest by the compiler across refactors. */
@Suppress("unused")
private fun RuntimeExtractor.describeHome(): String = runtimeRootHint.absolutePath
