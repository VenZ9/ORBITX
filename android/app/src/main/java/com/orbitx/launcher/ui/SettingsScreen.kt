package com.orbitx.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbitx.launcher.core.OrbitPaths
import com.orbitx.launcher.core.OrbitRenderer
import com.orbitx.launcher.core.RuntimeCatalog
import com.orbitx.launcher.data.Store

/**
 * Settings: RAM, renderer, resolution, game directory and the destructive reset.
 * Values are written straight through to [Store], which persists on every mutation.
 */
@Composable
fun SettingsScreen() {
    val state by Store.state.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    var rendererPicker by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        SectionHeader("Settings", "Defaults applied to new profiles and sessions.")

        // --- Memory -------------------------------------------------------------
        Column(Modifier.padding(horizontal = 16.dp)) {
            OrbitCard {
                Column {
                    Text("Memory allocation", style = MaterialTheme.typography.titleSmall, color = OrbitText)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${state.settings.ramMb} MB",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OrbitMint,
                    )
                    Slider(
                        value = state.settings.ramMb.toFloat(),
                        onValueChange = { v ->
                            val mb = (v / 256f).toInt() * 256
                            Store.updateSettings { it.ramMb = mb.coerceIn(512, 8192) }
                        },
                        valueRange = 512f..8192f,
                    )
                    Text(
                        "The launcher passes -Xmx/-Xms itself; version JSON values are removed first.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- Renderer -----------------------------------------------------------
        Column(Modifier.padding(horizontal = 16.dp)) {
            OrbitCard {
                Column {
                    Text("Renderer", style = MaterialTheme.typography.titleSmall, color = OrbitText)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        OrbitRenderer.fromId(state.settings.rendererId).title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrbitMint,
                    )
                    Text(
                        OrbitRenderer.fromId(state.settings.rendererId).description,
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Choose renderer") { rendererPicker = true }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- Resolution / assets ------------------------------------------------
        Column(Modifier.padding(horizontal = 16.dp)) {
            OrbitCard {
                Column {
                    Text("Display & assets", style = MaterialTheme.typography.titleSmall, color = OrbitText)
                    Spacer(Modifier.height(6.dp))
                    LabelValue("Resolution", "${state.settings.width} × ${state.settings.height}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("720p") {
                            Store.updateSettings { it.width = 1280; it.height = 720 }
                        }
                        GhostButton("1080p") {
                            Store.updateSettings { it.width = 1920; it.height = 1080 }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = !state.settings.skipAssets,
                            onCheckedChange = { on -> Store.updateSettings { it.skipAssets = !on } },
                        )
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Text(
                            "Download game assets",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrbitText,
                        )
                    }
                    Text(
                        "Turning this off skips the multi-hundred-MB asset download; the game " +
                            "will start without sounds and some textures.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- Storage ------------------------------------------------------------
        Column(Modifier.padding(horizontal = 16.dp)) {
            OrbitCard {
                Column {
                    Text("Storage", style = MaterialTheme.typography.titleSmall, color = OrbitText)
                    Spacer(Modifier.height(6.dp))
                    LabelValue("Game directory", OrbitPaths.gameDir.absolutePath)
                    LabelValue("Runtimes", OrbitPaths.runtimesDir.absolutePath)
                    LabelValue("Instances", OrbitPaths.instancesDir.absolutePath)
                    LabelValue("Device ABI", RuntimeCatalog.primaryAbi())
                    LabelValue("Java runtime packages", RuntimeCatalog.SUPPORTED.joinToString(", "))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(Modifier.padding(horizontal = 16.dp)) {
            GhostButton("Reset launcher data") { confirmReset = true }
            Spacer(Modifier.height(6.dp))
            Text(
                "Removes profiles, control layouts and settings. Installed game files and " +
                    "worlds on disk are not deleted.",
                style = MaterialTheme.typography.labelSmall,
                color = OrbitMuted,
            )
        }
    }

    if (rendererPicker) {
        AlertDialog(
            onDismissRequest = { rendererPicker = false },
            title = { Text("Renderer") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OrbitRenderer.entries.forEach { r ->
                        TextButton(onClick = {
                            Store.updateSettings { it.rendererId = r.id }
                            Store.currentProfile()?.let { p ->
                                Store.updateProfile(p.id) { it.rendererId = r.id }
                            }
                            rendererPicker = false
                        }) {
                            Column {
                                Text(r.title, color = OrbitText)
                                Text(r.description, style = MaterialTheme.typography.labelSmall, color = OrbitMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { rendererPicker = false }) { Text("Close") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset launcher data?") },
            text = { Text("Profiles, layouts and settings return to defaults. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { Store.resetAll(); confirmReset = false }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
