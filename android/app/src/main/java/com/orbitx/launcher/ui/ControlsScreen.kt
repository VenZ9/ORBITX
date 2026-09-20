package com.orbitx.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.orbitx.launcher.data.Control
import com.orbitx.launcher.data.Layout
import com.orbitx.launcher.data.Store

/**
 * The bindable actions. Kept as a plain list of names so the stored data stays a
 * simple string (and therefore forward-compatible if more are added later).
 */
val ControlActions = listOf(
    "JUMP", "SNEAK", "SPRINT", "ATTACK", "USE", "INVENTORY", "DROP",
    "SWAP_HANDS", "CHAT", "PAUSE", "PERSPECTIVE", "SLOT_1", "SLOT_2",
    "SLOT_3", "SLOT_4", "SLOT_5", "SLOT_6", "SLOT_7", "SLOT_8", "SLOT_9",
)

private const val MIN_SIZE = 0.06f
private const val MAX_SIZE = 0.5f

/**
 * Custom Controls editor.
 *
 * Geometry is stored normalized (0..1) against the *measured* canvas, so a layout is
 * resolution- and orientation-independent: the same percentages describe a portrait
 * 9:16 and a landscape 16:9 canvas, and dragging always uses the current canvas size.
 *
 * Two classes of bug are deliberately designed out here:
 *  1. Stale gesture state. Reading `control` directly inside a `pointerInput` lambda
 *     captures the value from the composition that created it; because the gesture
 *     modifier is keyed on the id alone it is never rebuilt, so drag deltas get added
 *     to a frozen origin and the control stops following the finger. Every gesture
 *     here reads through `rememberUpdatedState`, so each event sees the live value.
 *  2. Pixel/dp confusion. The canvas is measured in pixels; layout offsets/sizes must
 *     be converted with the current density before being handed to Compose, otherwise
 *     rendered geometry and touch coordinates diverge by the density factor.
 */
@Composable
fun ControlsScreen() {
    val state by Store.state.collectAsState()
    val layout = state.layouts.firstOrNull { it.id == state.selectedLayoutId }
    val density = LocalDensity.current

    // Editor works on a draft; Save commits it. Keyed on the layout id so switching
    // layouts (or resetting) reloads rather than keeping the previous draft.
    var draft by remember(layout?.id) { mutableStateOf(layout?.controls?.toList() ?: emptyList()) }
    var canvas by remember { mutableStateOf(IntSize.Zero) }
    var selectedId by remember(layout?.id) { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingName by remember { mutableStateOf<String?>(null) }

    val liveDraft = rememberUpdatedState(draft)
    val liveCanvas = rememberUpdatedState(canvas)
    fun update(mutator: (List<Control>) -> List<Control>) { draft = mutator(liveDraft.value) }

    fun Control.clampedPosition(): Control {
        val cx = x.coerceIn(0f, (1f - w).coerceAtLeast(0f))
        val cy = y.coerceIn(0f, (1f - h).coerceAtLeast(0f))
        return copy(x = cx, y = cy)
    }

    if (layout == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No control layout selected.", color = OrbitMuted)
        }
        return
    }

    val selected = draft.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            "Custom Controls",
            "Drag to move, drag the corner to resize. Coordinates are stored as 0..1 of the canvas.",
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

        Spacer(Modifier.height(8.dp))

        // --- Canvas -------------------------------------------------------------
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF04060A))
                    .border(1.dp, OrbitSurfaceHi, RoundedCornerShape(14.dp))
                    .onSizeChanged { canvas = it },
            ) {
                // Grid guides, purely visual.
                Box(Modifier.fillMaxWidth().height(1.dp).offset(y = 0.dp).background(OrbitSurfaceHi))

                draft.forEach { c ->
                    // key() keeps remembered per-item state bound to the right control
                    // when controls are added or removed.
                    key(c.id) {
                        val isSelected = c.id == selectedId
                        val cState = rememberUpdatedState(c)

                        Box(
                            Modifier
                                .offset(
                                    x = with(density) { (c.x * canvas.width).toDp() },
                                    y = with(density) { (c.y * canvas.height).toDp() },
                                )
                                .size(
                                    width = with(density) { (c.w * canvas.width).toDp() },
                                    height = with(density) { (c.h * canvas.height).toDp() },
                                )
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    (if (isSelected) OrbitMint else OrbitViolet)
                                        .copy(alpha = 0.20f * c.opacity)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrbitMint else OrbitViolet.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(9.dp),
                                )
                                .clickable { selectedId = c.id }
                                // Keyed on the id AND the measured canvas: the handler is
                                // rebuilt when the canvas changes (rotation) so the math
                                // always divides by the live canvas dimensions.
                                .pointerInput(c.id, canvas) {
                                    detectDragGestures(
                                        onDragStart = { selectedId = c.id },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val w = liveCanvas.value.width.toFloat()
                                            val h = liveCanvas.value.height.toFloat()
                                            if (w <= 0f || h <= 0f) return@detectDragGestures
                                            // positionChange() is the movement since the
                                            // previous event, so accumulating it against
                                            // the LIVE value tracks the finger exactly.
                                            val dx = change.positionChange().x / w
                                            val dy = change.positionChange().y / h
                                            val cur = cState.value
                                            update { list ->
                                                list.map {
                                                    if (it.id != cur.id) it
                                                    else it.copy(
                                                        x = (it.x + dx).coerceIn(0f, (1f - it.w).coerceAtLeast(0f)),
                                                        y = (it.y + dy).coerceIn(0f, (1f - it.h).coerceAtLeast(0f)),
                                                    )
                                                }
                                            }
                                        },
                                    )
                                },
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    c.action.replace('_', ' '),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OrbitText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }

                            // Resize handle. Declared inside the control so it is hit
                            // before the parent's drag detector.
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .pointerInput(c.id, canvas) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            val w = liveCanvas.value.width.toFloat()
                                            val h = liveCanvas.value.height.toFloat()
                                            if (w <= 0f || h <= 0f) return@detectDragGestures
                                            val dw = change.positionChange().x / w
                                            val dh = change.positionChange().y / h
                                            val cur = cState.value
                                            update { list ->
                                                list.map {
                                                    if (it.id != cur.id) it
                                                    else {
                                                        val nw = (it.w + dw).coerceIn(MIN_SIZE, MAX_SIZE)
                                                        val nh = (it.h + dh).coerceIn(MIN_SIZE, MAX_SIZE)
                                                        // Re-clamp the position so growing a
                                                        // control near an edge cannot push
                                                        // it outside the canvas.
                                                        it.copy(
                                                            w = nw,
                                                            h = nh,
                                                            x = it.x.coerceIn(0f, (1f - nw).coerceAtLeast(0f)),
                                                            y = it.y.coerceIn(0f, (1f - nh).coerceAtLeast(0f)),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isSelected) OrbitMint else OrbitMuted),
                                )
                            }
                        }
                    }
                }

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
                                c.action.replace('_', ' '),
                                style = MaterialTheme.typography.titleSmall,
                                color = OrbitText,
                            )
                            IconButton(onClick = {
                                update { list -> list.filterNot { it.id == c.id } }
                                selectedId = null
                            }) { Icon(Icons.Filled.Delete, "Delete", tint = OrbitMuted) }
                        }
                        LabelValue("Position", "%.3f, %.3f".format(c.x, c.y))
                        LabelValue("Size", "%.3f × %.3f".format(c.w, c.h))
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
                enabled = draft != layout.controls,
                onClick = {
                    Store.updateLayout(layout.id) { it.controls = draft.toMutableList() }
                },
            ) { Text("Save", color = if (draft != layout.controls) OrbitMint else OrbitMuted) }
        }
        Text(
            "Reset restores the layout you are editing. Built-in Default and PvP restore " +
                "their stock bindings; a custom layout is cleared.",
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
                            GhostButton(a.replace('_', ' ')) {
                                // Deterministic cascade instead of stacking every new
                                // control at the same point: step across, then wrap down.
                                val i = draft.size
                                val col = i % 6
                                val row = (i / 6) % 5
                                val nc = Control(
                                    action = a,
                                    x = (0.04f + col * 0.16f),
                                    y = (0.06f + row * 0.17f),
                                    w = 0.13f,
                                    h = 0.13f,
                                ).clampedPosition()
                                draft = draft + nc
                                selectedId = nc.id
                                showAdd = false
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "New controls are placed at the next free slot and are always fully " +
                            "inside the canvas.",
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
                    draft = Store.currentLayout()?.controls?.toList() ?: emptyList()
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
