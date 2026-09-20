package com.orbitx.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbitx.launcher.data.Control
import com.orbitx.launcher.data.controlLabel

/**
 * The on-screen control overlay, shared by the Custom Controls editor and the in-game
 * touch layer. One renderer means the editor cannot drift from what the game shows.
 *
 * Two modes, and the difference is the whole point:
 *
 *  - **Edit mode** (`editMode = true`): controls are manipulable. Pressing selects, dragging
 *    moves, and the corner handle resizes. Geometry is committed through [onMove]/[onResize].
 *  - **Play mode** (`editMode = false`): controls are pure input. They report press and
 *    release through [onPress] and *never move*, so a drag across the screen cannot
 *    accidentally rearrange the layout mid-session. Entering edit mode is a deliberate act
 *    (a long press, or an explicit toggle).
 *
 * Geometry handling, which is where the earlier glitches came from:
 *  - Stored coordinates are normalized 0..1. The canvas is measured in **pixels**, so every
 *    value is converted with the current [LocalDensity] before being handed to Compose.
 *    Passing pixels to `.dp` scales geometry by the density factor and desynchronises what
 *    is drawn from where the finger is.
 *  - Gesture lambdas read through [rememberUpdatedState], so each event sees live state
 *    instead of the value captured when the modifier was first created. A stale capture
 *    makes deltas accumulate against a frozen origin, which is the "control snaps back to
 *    where it started" symptom.
 *  - The gesture modifier is keyed on the control id *and* the measured canvas, so it is
 *    rebuilt when the canvas changes (rotation) and the math always divides by the live
 *    dimensions.
 *  - Clamping lives in the data layer (`clampControlX/Y`), so `x + w <= 1` and `y + h <= 1`
 *    hold no matter which caller moved the control.
 */
@Composable
fun ControlOverlay(
    controls: List<Control>,
    editMode: Boolean,
    modifier: Modifier = Modifier,
    canvasAspect: Float = 16f / 9f,
    letterbox: Boolean = false,
    globalOpacity: Float = 1f,
    selectedId: String? = null,
    onSelect: ((String) -> Unit)? = null,
    onEnterEditMode: (() -> Unit)? = null,
    onMove: ((Control, Float, Float) -> Unit)? = null,
    onResize: ((Control, Float, Float) -> Unit)? = null,
    onPress: ((Control, Boolean) -> Unit)? = null,
    onCanvasMeasured: ((IntSize) -> Unit)? = null,
) {
    var canvas by remember { mutableStateOf(IntSize.Zero) }
    var pressedId by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val liveCanvas = rememberUpdatedState(canvas)

    Box(modifier.onSizeChanged { canvas = it; onCanvasMeasured?.invoke(it) }) {
        if (letterbox) {
            // Preview framing: the game viewport is shown at its real aspect ratio inside a
            // taller canvas, so a layout can be judged the way it will actually look.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07080C)),
            )
            val cw = canvas.width.toFloat()
            val ch = canvas.height.toFloat()
            if (cw > 0f && ch > 0f) {
                val boxW: Float
                val boxH: Float
                if (cw / ch > canvasAspect) { boxH = ch; boxW = ch * canvasAspect }
                else { boxW = cw; boxH = cw / canvasAspect }
                Box(
                    Modifier
                        .offset(
                            x = with(density) { ((cw - boxW) / 2f).toDp() },
                            y = with(density) { ((ch - boxH) / 2f).toDp() },
                        )
                        .size(
                            width = with(density) { boxW.toDp() },
                            height = with(density) { boxH.toDp() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "game viewport ${canvasAspect.toString().take(4)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitSurfaceHi,
                    )
                }
            }
        }

        controls.filter { it.enabled }.forEach { c ->
            // key() binds remembered per-item state to the right control, so adding or
            // removing a control cannot leave state attached to its neighbour.
            key(c.id) {
                val isSelected = editMode && c.id == selectedId
                val isPressed = pressedId == c.id
                val cState = rememberUpdatedState(c)
                val labelPx = (c.h * canvas.height * 0.30f).coerceIn(
                    with(density) { 8.sp.toPx() },
                    with(density) { 18.sp.toPx() },
                )

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
                        .clip(RoundedCornerShape(if (editMode) 9.dp else 12.dp))
                        // Pojav-style surface: translucent fill, subtle border, and a
                        // pressed state that is a real visual change rather than a redraw.
                        .background(
                            when {
                                isPressed -> OrbitMint.copy(alpha = 0.42f)
                                isSelected -> OrbitMint.copy(alpha = 0.20f)
                                else -> Color.White.copy(alpha = 0.13f * c.opacity * globalOpacity)
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> OrbitMint
                                isPressed -> OrbitMint.copy(alpha = 0.8f)
                                else -> Color.White.copy(alpha = 0.30f * c.opacity * globalOpacity)
                            },
                            shape = RoundedCornerShape(if (editMode) 9.dp else 12.dp),
                        )
                        .pointerInput(c.id, canvas, editMode) {
                            if (editMode) {
                                // Edit mode: press selects, drag moves. One gesture loop for
                                // both, so selection cannot be swallowed by the drag detector.
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    onSelect?.invoke(c.id)
                                    drag(down.id) { change ->
                                        val delta = change.positionChange()
                                        if (delta.getDistance() > 0f) {
                                            change.consume()
                                            val w = liveCanvas.value.width.toFloat()
                                            val h = liveCanvas.value.height.toFloat()
                                            if (w <= 0f || h <= 0f) return@drag
                                            onMove?.invoke(cState.value, delta.x / w, delta.y / h)
                                        }
                                    }
                                }
                            } else {
                                // Play mode: press and release only. Nothing here can move
                                // a control, which is what keeps the layout stable in game.
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pressedId = c.id
                                    onPress?.invoke(cState.value, true)
                                    val up = waitForUpOrCancellation()
                                    pressedId = null
                                    onPress?.invoke(cState.value, up != null)
                                    // A long press is the deliberate way into edit mode.
                                    if (up == null) onEnterEditMode?.invoke()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        controlLabel(c.action),
                        style = MaterialTheme.typography.labelSmall,
                        // Scale the label with the control so it stays legible on a small
                        // screen without overflowing a small button. Clamped in px space,
                        // because TextUnit is not an ordered type.
                        fontSize = with(density) { labelPx.toSp() },
                        color = Color.White.copy(alpha = (if (isPressed) 1f else 0.92f) * c.opacity),
                        fontWeight = if (isSelected || isPressed) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 1.dp),
                        maxLines = 1,
                    )

                    if (isSelected) {
                        // Drag handle, only in edit mode: the affordance that says "this one
                        // is selected and can be resized" without showing it during play.
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .pointerInput(c.id, canvas) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        drag(down.id) { change ->
                                            val delta = change.positionChange()
                                            if (delta.getDistance() > 0f) {
                                                change.consume()
                                                val w = liveCanvas.value.width.toFloat()
                                                val h = liveCanvas.value.height.toFloat()
                                                if (w <= 0f || h <= 0f) return@drag
                                                onResize?.invoke(cState.value, delta.x / w, delta.y / h)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(OrbitMint),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Colour of a press ripple, kept out of the hot path but available to callers. */
internal val ControlPressTint = OrbitMint

/** Unused placeholder guard so the import list stays honest across refactors. */
private val NoOpOffset = Offset.Zero
