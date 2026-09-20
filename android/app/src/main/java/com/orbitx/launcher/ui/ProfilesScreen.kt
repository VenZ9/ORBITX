package com.orbitx.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbitx.launcher.core.OrbitRenderer
import com.orbitx.launcher.data.Profile
import com.orbitx.launcher.data.Store

/**
 * Profiles: unlimited offline identities. Each profile carries its own version,
 * RAM ceiling and renderer, so a player can keep several independent setups.
 */
@Composable
fun ProfilesScreen() {
    val state by Store.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<Profile?>(null) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
    ) {
        SectionHeader(
            "Profiles",
            "Offline identities with locally generated UUIDs. No account required.",
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostButton("New profile") { newName = ""; showAdd = true }
        }

        state.profiles.forEach { p ->
            val selected = p.id == state.selectedProfileId
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OrbitCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    p.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrbitText,
                                )
                                Spacer(Modifier.size(8.dp))
                                if (selected) Pill("ACTIVE")
                            }
                            IconButton(onClick = { confirmDelete = p }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = OrbitMuted)
                            }
                        }
                        LabelValue("UUID", p.uuid)
                        LabelValue(
                            "Version",
                            p.versionId.takeIf { it.isNotBlank() } ?: "not set",
                            if (p.versionId.isBlank()) OrbitMuted else OrbitMint,
                        )
                        LabelValue("RAM", "${p.ramMb} MB")
                        LabelValue("Renderer", OrbitRenderer.fromId(p.rendererId).title)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { Store.selectProfile(p.id) }) {
                                Text(if (selected) "Selected" else "Use profile")
                            }
                            TextButton(onClick = {
                                Store.updateProfile(p.id) { it.ramMb = (it.ramMb + 512).coerceIn(512, 8192) }
                            }) { Text("+512 MB") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New offline profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Player name") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The UUID is derived from the name (OfflinePlayer MD5), matching what " +
                            "the game uses for unauthenticated players.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrbitMuted,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val p = Profile(name = newName.trim())
                        Store.addProfile(p)
                        Store.selectProfile(p.id)
                        showAdd = false
                    },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
        )
    }

    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${target.name}?") },
            text = { Text("The world saves are not removed; only the launcher profile entry is.") },
            confirmButton = {
                TextButton(onClick = { Store.removeProfile(target.id); confirmDelete = null }) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep") } },
        )
    }
}
