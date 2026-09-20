package com.orbitx.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbitx.launcher.core.Installer
import com.orbitx.launcher.core.MojangApi
import com.orbitx.launcher.core.OrbitLog
import com.orbitx.launcher.core.Progress
import com.orbitx.launcher.data.InstalledVersion
import com.orbitx.launcher.data.Store
import kotlinx.coroutines.launch

/**
 * Versions: what is installed, plus the download/loader actions.
 * Installed state is re-read after each operation so the list cannot go stale.
 */
@Composable
fun VersionsScreen() {
    val state by Store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(Installer.installedVersions()) }
    var available by remember { mutableStateOf<List<MojangApi.VersionSummary>>(emptyList()) }
    var stage by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Progress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun refresh() { installed = Installer.installedVersions() }

    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        SectionHeader("Versions", "Install Minecraft versions and mod loaders, then bind one to a profile.")

        OrbitCard(Modifier.padding(horizontal = 16.dp)) {
            Column {
                Text("Installed (${installed.size})", style = MaterialTheme.typography.titleSmall, color = OrbitText)
                Spacer(Modifier.height(6.dp))
                if (installed.isEmpty()) {
                    Text(
                        "Nothing installed yet. Fetch the version list below and install one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrbitMuted,
                    )
                } else {
                    installed.forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(v.id, style = MaterialTheme.typography.bodyMedium, color = OrbitText)
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    Pill(v.loader)
                                }
                                Text(
                                    if (v.hasJar) "client jar present" else "jar missing — reinstall",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (v.hasJar) OrbitMuted else OrbitError,
                                )
                            }
                            TextButton(onClick = {
                                val p = Store.currentProfile() ?: return@TextButton
                                Store.updateProfile(p.id) { it.versionId = v.id }
                            }) { Text("Use") }
                        }
                    }
                }
            }
        }

        if (busy) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                progress?.takeIf { it.total > 0 }?.let {
                    ProgressRow(it.fraction, it.currentName)
                } ?: Text(stage, style = MaterialTheme.typography.bodySmall, color = OrbitMint)
            }
        } else if (stage.isNotBlank()) {
            Text(
                stage,
                style = MaterialTheme.typography.bodySmall,
                color = OrbitMint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = OrbitError,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GhostButton(if (available.isEmpty()) "Fetch version list" else "Refresh list", !busy) {
                scope.launch {
                    busy = true; error = null; stage = "Fetching version manifest…"
                    runCatching { Installer.listAvailable() }
                        .onSuccess { available = it; stage = "${it.size} versions available" }
                        .onFailure { error = "manifest: ${it.message}"; OrbitLog.e("manifest", it) }
                    busy = false; progress = null
                }
            }
        }

        if (available.isNotEmpty()) {
            Text(
                "Available releases",
                style = MaterialTheme.typography.titleSmall,
                color = OrbitText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
                items(available.filter { it.type == "release" }.take(60), key = { it.id }) { v ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(v.id, style = MaterialTheme.typography.bodyMedium, color = OrbitText)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(enabled = !busy, onClick = {
                                scope.launch {
                                    busy = true; error = null; progress = null
                                    runCatching {
                                        Installer.installVanilla(
                                            id = v.id,
                                            jsonUrl = v.url,
                                            onStage = { stage = it },
                                            onProgress = { progress = it },
                                        )
                                    }.onSuccess {
                                        refresh()
                                        Store.currentProfile()?.let { p ->
                                            if (p.versionId.isBlank()) Store.updateProfile(p.id) { it.versionId = v.id }
                                        }
                                        stage = "Installed ${v.id}"
                                    }.onFailure {
                                        error = "${v.id}: ${it.message}"
                                        OrbitLog.e("install ${v.id}", it)
                                    }
                                    busy = false; progress = null
                                }
                            }) { Text("Install") }

                            TextButton(enabled = !busy, onClick = {
                                scope.launch {
                                    busy = true; error = null; progress = null
                                    runCatching {
                                        val loaders = Installer.fabricLoaders(v.id)
                                        val chosen = loaders.firstOrNull { it.stable } ?: loaders.firstOrNull()
                                            ?: throw IllegalStateException("no Fabric loader published for ${v.id}")
                                        stage = "Fabric ${chosen.version}"
                                        Installer.installFabric(v.id, chosen.version) { stage = it }
                                    }.onSuccess { refresh() }
                                        .onFailure {
                                            error = "fabric: ${it.message}"
                                            OrbitLog.e("fabric", it)
                                        }
                                    busy = false; progress = null
                                }
                            }) { Text("+ Fabric") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Downloads are hash-verified against Mojang's published SHA-1 values. " +
                "The JVM runtime is fetched on first launch (~25-40 MB).",
            style = MaterialTheme.typography.labelSmall,
            color = OrbitMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            "Note: for a real session, Forge uses the version's installer jar and the " +
                "bundled Android LWJGL build must be present (see LICENSE-THIRD-PARTY.md).",
            style = MaterialTheme.typography.labelSmall,
            color = OrbitMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
