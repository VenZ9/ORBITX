package com.orbitx.launcher.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbitx.launcher.core.OrbitRenderer
import com.orbitx.launcher.core.RuntimeCatalog
import com.orbitx.launcher.data.Store

/** Home: identity, selected version, and the launch action. */
@Composable
fun HomeScreen(onNavigate: (OrbitScreen) -> Unit) {
    val state by Store.state.collectAsState()
    val profile = state.profiles.firstOrNull { it.id == state.selectedProfileId }
        ?: state.profiles.firstOrNull()

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
            text = "PLAY",
            enabled = !profile?.versionId.isNullOrBlank(),
            onClick = { onNavigate(OrbitScreen.Versions) },
        )

        Text(
            "Install a version on the Versions tab, then start the session from the game screen.",
            style = MaterialTheme.typography.bodySmall,
            color = OrbitMuted,
        )

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
                    LabelValue("Storage", "local JSON (offline)")
                }
            }
        }
    }
}
