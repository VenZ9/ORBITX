package com.orbitx.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbitx.launcher.data.Control
import com.orbitx.launcher.data.ControlActions
import com.orbitx.launcher.data.Layout
import com.orbitx.launcher.data.Store
import com.orbitx.launcher.data.clampControlX
import com.orbitx.launcher.data.clampControlY
import com.orbitx.launcher.data.clampIntoCanvas
import com.orbitx.launcher.data.companionPlacement
import com.orbitx.launcher.data.controlLabel
import com.orbitx.launcher.data.movedBy
import com.orbitx.launcher.data.resizedBy

/**
 * Custom Controls editor.
 *
 * Geometry is stored normalized (0..1) against the *measured* canvas, so one layout
 * describes both a portrait 9:16 and a landscape 16:9 canvas and dragging always divides
 * by the live dimensions. Rendering, gesture math and clamping all live in
 * [ControlOverlay] and the `clampControl*` helpers, so the editor and the in-game overlay
 * cannot disagree about where a control is.
 *
 * Editing is gated behind an explicit edit mode: outside it the canvas is a live preview
 * and controls are inert, which is what PojavLauncher and Zalith Launcher do and what stops
 * a stray drag from rearranging the layout while playing.
 */
@Composable
fun ControlsScreen() {
    val state by Store.state.collectAsState()
    val layout = state.layouts.firstOrNull { it.id == state.selectedLayoutId }

    // Editor works on a draft; Save commits it. Keyed on the layout id so switching
    // layouts (or resetting) reloads rather than keeping the previous draft. The type
    // parameter is explicit: `?: emptyList()` alone leaves Kotlin inferring Any?.
    var draft by remember(layout?.id) {
        mutableStateOf<List<Control>>(layout?.controls?.map { it.clampIntoCanvas() } ?: emptyList())
    }
    var selectedId by remember(layout?.id) { mutableStateOf<String?>(null) }
    var editMode by remember(layout?.id) { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingName by remember { mutableStateOf<String?>(null) }

    val liveDraft = rememberUpdatedState(draft)
    fun update(mutator: (List<Control>) -> List<Control>) { draft = mutator(liveDraft.value) }

    if (layout == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No control layout selected.", color = OrbitMuted)
        }
        return
    }

    val selected = draft.firstOrNull { it.id == selectedId }
    val dirty = draft != layout.controls

    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            "Custom Controls",
            "Match PojavLauncher's arrangement, or make your own. Coordinates are stored " +
                "as 0..1 of the canvas, so a layout fits any screen.",
        )

        // --- Layout chooser -----------------------------------------------------
        LazyRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.layouts, key = { it.id }) { l ->
                val active = l.id == layout.id
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) OrbitMint.copy(alpha = 0.16f) else OrbitSurfaceHi)
                        .clickable { Store.selectLayout(l.id) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        l.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) OrbitMint else OrbitMuted,
                    )
                }
            }
            item {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OrbitSurfaceHi)
                        .clickable { pendingName = "" }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("+ New", style = MaterialTheme.typography.labelMedium, color = OrbitMuted)
                }
            }
        }

        // --- Edit mode toggle ---------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (editMode) OrbitMint.copy(alpha = 0.20f) else OrbitSurfaceHi)
                    .clickable {
                        editMode = !editMode
                        if (!editMode) selectedId = null
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    if (editMode) "Editing — tap to lock" else "Preview (tap to edit)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (editMode) OrbitMint else OrbitMuted,
                    fontWeight = if (editMode) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Text(
                if (editMode) "Drag to move, corner handle to resize."
                else "Controls are inert here. Long-press one in game to edit.",
                style = MaterialTheme.typography.labelSmall,
                color = OrbitMuted,
            )
        }

        // --- Canvas -------------------------------------------------------------
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
            ControlOverlay(
                controls = draft,
                editMode = editMode,
                globalOpacity = 0.85f,
                selectedId = selectedId,
                onSelect = { selectedId = it },
                onMove = { c, dx, dy ->
                    update { list -> list.map { if (it.id == c.id) it.movedBy(dx, dy) else it } }
                },
                onResize = { c, dw, dh ->
                    update { list -> list.map { if (it.id == c.id) it.resizedBy(dw, dh) else it } }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF04060A)),
            )

            if (draft.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No controls yet — tap Add control below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrbitMuted,
                    )
                }
            }
        }

        // --- Inspector ----------------------------------------------------------
        selected?.let { c ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OrbitCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                controlLabel(c.action),
                                style = MaterialTheme.typography.titleSmall,
                                color = OrbitText,
                            )
                            IconButton(onClick = {
                                update { list -> list.filterNot { it.id == c.id } }
                                selectedId = null
                            }) { Icon(Icons.Filled.Delete, "Delete", tint = OrbitMuted) }
                        }
                        LabelValue("Position", "%.3f, %.3f".format(c.x, c.y))
                        LabelValue(
                            "Size",
                            "%.3f × %.3f".format(c.w, c.h),
                        )
                        // Show the bounds invariant explicitly: this is the value that used
                        // to be violated (a control past x + w <= 1) with no visible cause.
                        LabelValue(
                            "Right / bottom edge",
                            "%.3f, %.3f".format(c.x + c.w, c.y + c.h),
                            if (c.x + c.w <= 1.001f && c.y + c.h <= 1.001f) OrbitMint else OrbitError,
                        )
                        Text("Opacity", style = MaterialTheme.typography.labelSmall, color = OrbitMuted)
                        Slider(
                            value = c.opacity,
                            onValueChange = { v ->
                                update { list -> list.map { if (it.id == c.id) it.copy(opacity = v) else it } }
                            },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = c.enabled,
                                onCheckedChange = { on ->
                                    update { list -> list.map { if (it.id == c.id) it.copy(enabled = on) else it } }
                                },
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Enabled", style = MaterialTheme.typography.bodySmall, color = OrbitText)
                        }
                        GhostButton("Re-clamp into canvas") {
                            update { list ->
                                list.map { if (it.id == c.id) it.copy(
                                    x = clampControlX(it.x, it.w),
                                    y = clampControlY(it.y, it.h),
                                ) else it }
                            }
                        }
                    }
                }
            }
        }

        // --- Actions ------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostButton("Add control") { showAdd = true }
            GhostButton("Reset") { confirmReset = true }
            Spacer(Modifier.fillMaxWidth().weight(1f))
            TextButton(
                enabled = dirty,
                onClick = {
                    Store.updateLayout(layout.id) { it.controls = draft.toMutableList() }
                },
            ) { Text("Save", color = if (dirty) OrbitMint else OrbitMuted) }
        }
        Text(
            "Reset restores the layout you are editing — Default and PvP go back to their " +
                "stock arrangement, and a custom layout is cleared. Other layouts are untouched.",
            style = MaterialTheme.typography.labelSmall,
            color = OrbitMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add control") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text("Choose an action", style = MaterialTheme.typography.labelMedium, color = OrbitMuted)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ControlActions) { a ->
                            GhostButton(controlLabel(a)) {
                                // Deterministic cascade instead of stacking every new control
                                // at the same point, clamped so it is always fully visible.
                                val nc = Control(action = a, x = 0f, y = 0f, w = 0.13f, h = 0.13f)
                                    .companionPlacement(draft.size)
                                draft = draft + nc
                                selectedId = nc.id
                                editMode = true
                                showAdd = false
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "New controls land in the next free slot and are always fully inside " +
                            "the canvas.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitMuted,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text("Done") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset \"${layout.name}\"?") },
            text = { Text("Only this layout is reset. Other layouts are untouched.") },
            confirmButton = {
                TextButton(onClick = {
                    Store.resetSelectedLayout()
                    draft = Store.currentLayout()?.controls?.map { it.clampIntoCanvas() } ?: emptyList()
                    selectedId = null
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }

    pendingName?.let { value ->
        AlertDialog(
            onDismissRequest = { pendingName = null },
            title = { Text("New control layout") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { pendingName = it },
                    label = { Text("Layout name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = value.isNotBlank(),
                    onClick = {
                        Store.addLayout(Layout(name = value.trim(), controls = mutableListOf()))
                        pendingName = null
                    },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { pendingName = null }) { Text("Cancel") } },
        )
    }
}
