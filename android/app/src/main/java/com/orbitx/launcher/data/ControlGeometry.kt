package com.orbitx.launcher.data

/**
 * Control geometry, in normalized canvas space.
 *
 * Every control is stored as fractions of the canvas (0..1), never in pixels or dp, so a
 * layout is resolution- and orientation-independent: the same numbers describe a portrait
 * 9:16 canvas and a landscape 16:9 one. These helpers are the single place the bounds rule
 * lives — `x + w <= 1` and `y + h <= 1` — so the editor, the in-game overlay and the
 * loader that reads a saved layout all agree.
 *
 * The bug this exists to prevent: a control sitting at the right edge with a non-zero
 * width (SPRINT in the Default layout was the visible one) rendering partly outside the
 * canvas, because its size was never subtracted from its maximum position.
 */

/** Smallest control, as a fraction of the canvas. */
const val MIN_CONTROL_SIZE = 0.06f

/** Largest control, as a fraction of the canvas. */
const val MAX_CONTROL_SIZE = 0.5f

/** Largest legal x for a control of width [w]. */
fun clampControlX(x: Float, w: Float): Float = x.coerceIn(0f, (1f - w).coerceAtLeast(0f))

/** Largest legal y for a control of height [h]. */
fun clampControlY(y: Float, h: Float): Float = y.coerceIn(0f, (1f - h).coerceAtLeast(0f))

/** Pull a control fully inside the canvas without changing its size. */
fun Control.clampIntoCanvas(): Control = copy(
    x = clampControlX(x, w),
    y = clampControlY(y, h),
)

/** Move by a normalized delta, clamped to the canvas. */
fun Control.movedBy(dx: Float, dy: Float): Control = copy(
    x = clampControlX(x + dx, w),
    y = clampControlY(y + dy, h),
)

/**
 * Resize by a normalized delta, then re-clamp the position.
 *
 * The re-clamp is the part that is easy to forget: growing a control near an edge pushes
 * its far side past the canvas unless the position is pulled back in afterwards.
 */
fun Control.resizedBy(dw: Float, dh: Float): Control {
    val nw = (w + dw).coerceIn(MIN_CONTROL_SIZE, MAX_CONTROL_SIZE)
    val nh = (h + dh).coerceIn(MIN_CONTROL_SIZE, MAX_CONTROL_SIZE)
    return copy(w = nw, h = nh, x = clampControlX(x, nw), y = clampControlY(y, nh))
}

/**
 * The first free slot in a simple cascade, so a newly added control is visible and does
 * not land on top of an existing one.
 */
fun Control.companionPlacement(index: Int): Control {
    val col = index % 6
    val row = (index / 6) % 5
    return copy(x = 0.04f + col * 0.155f, y = 0.05f + row * 0.175f).clampIntoCanvas()
}
