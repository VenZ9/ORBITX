package com.orbitx.launcher.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbitx.launcher.GameActivity
import com.orbitx.launcher.core.LaunchPreflight
import com.orbitx.launcher.core.LaunchReport
import com.orbitx.launcher.core.NativeBridge
import com.orbitx.launcher.core.OrbitLog
import com.orbitx.launcher.core.OrbitRenderer
import com.orbitx.launcher.core.RuntimeCatalog
import com.orbitx.launcher.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home: identity, selected version, and the launch action.
 *
 * The Play button starts a session for real. It runs a preflight first (on a background
 * thread, because it touches the filesystem and the version json), then either launches
 * [GameActivity] or shows exactly what is missing. Either way the press produces a
 * visible result: a launch that silently does nothing is the failure mode this screen is
 * written to make impossible.
 */
@Composable
fun HomeScreen(onNavigate: (OrbitScreen) -> Unit) {
    val state by Store.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profile = state.profiles.firstOrNull { it.id == state.selectedProfileId }
        ?: state.profiles.firstOrNull()

    var checking by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<LaunchReport?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var failureMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrbitViolet.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text("OX", color = OrbitViolet, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    "OrbitX Launcher",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OrbitText,
                )
                Text(
                    "Minecraft: Java Edition for Android",
                    style = MaterialTheme.typography.bodySmall,
                    color = OrbitMuted,
                )
            }
        }

        OrbitCard {
            Column {
                Text("PROFILE", style = MaterialTheme.typography.labelSmall, color = OrbitMuted)
                Text(
                    profile?.name ?: "No profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OrbitText,
                )
                Spacer(Modifier.height(6.dp))
                LabelValue("Offline UUID", profile?.uuid?.take(18)?.plus("…") ?: "–")
                LabelValue(
                    "Version",
                    profile?.versionId?.takeIf { it.isNotBlank() } ?: "not selected",
                    if (profile?.versionId.isNullOrBlank()) OrbitError else OrbitMint,
                )
                LabelValue("RAM", "${profile?.ramMb ?: 2048} MB")
                LabelValue(
                    "Renderer",
                    OrbitRenderer.fromId(profile?.rendererId).title,
                )
            }
        }

        PrimaryButton(
            text = if (checking) "CHECKING…" else "PLAY",
            enabled = !checking && profile != null,
            onClick = {
                if (profile == null) return@PrimaryButton
                checking = true
                scope.launch {
                    val r = withContext(Dispatchers.IO) {
                        runCatching { LaunchPreflight.inspect(profile.id) }
                            .onFailure { OrbitLog.e("preflight failed", it) }
                            .getOrNull()
                    }
                    checking = false
                    if (r == null) {
                        // Never a silent no-op: surface the failure itself.
                        report = null
                        showReport = false
                        failureMsg = "The launch check itself failed. See " +
                            "orbitx/logs/orbitx.log for the stack trace."
                        return@launch
                    }
                    OrbitLog.i("preflight: canLaunch=${r.canLaunch}; ${r.summary()}")
                    report = r
                    if (r.canLaunch) {
                        context.startActivity(
                            Intent(context, GameActivity::class.java).apply {
                                putExtra(GameActivity.EXTRA_PROFILE_ID, profile.id)
                                putExtra(GameActivity.EXTRA_VERSION_ID, r.versionId)
                            },
                        )
                    } else {
                        showReport = true
                    }
                }
            },
        )

        if (checking) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = OrbitMint,
                trackColor = OrbitSurfaceHi,
            )
        }

        Text(
            "Play runs the preflight checks, then starts the session. If something is " +
                "missing you get the exact reason instead of nothing happening.",
            style = MaterialTheme.typography.bodySmall,
            color = OrbitMuted,
        )

        // Surface the native-bridge limitation without waiting for a launch attempt: a
        // build without the EGL bridge can start a JVM but cannot present a frame.
        if (!NativeBridge.available) {
            OrbitCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = OrbitError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Diagnostics",
                            style = MaterialTheme.typography.titleSmall,
                            color = OrbitText,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        NativeBridge.missingHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitMuted,
                    )
                }
            }
        }

        if (state.settings.showDiagnostics) {
            OrbitCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, null, tint = OrbitMint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Runtime diagnostics",
                            style = MaterialTheme.typography.titleSmall,
                            color = OrbitText,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LabelValue("Device ABI", RuntimeCatalog.primaryAbi())
                    LabelValue("Profiles", state.profiles.size.toString())
                    LabelValue("Control layouts", state.layouts.size.toString())
                    LabelValue("Native bridge", if (NativeBridge.available) "present" else "not built in")
                    LabelValue("Storage", "local JSON (offline)")
                }
            }
        }
    }

    if (showReport) {
        val r = report
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Can't start \"${r?.versionId ?: "session"}\"") },
            text = {
                Column {
                    Text(
                        r?.summary() ?: "The instance is incomplete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrbitText,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "After fixing this, press Play again.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitMuted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showReport = false
                    onNavigate(OrbitScreen.Versions)
                }) { Text("Open Versions") }
            },
            dismissButton = {
                TextButton(onClick = { showReport = false }) { Text("Close") }
            },
        )
    }

    failureMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { failureMsg = null },
            title = { Text("Launch check failed") },
            text = { Text(msg, style = MaterialTheme.typography.bodySmall, color = OrbitText) },
            confirmButton = { TextButton(onClick = { failureMsg = null }) { Text("Close") } },
        )
    }
}
