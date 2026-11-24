package app.katari.presentation.kit.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.pointer.TouchpadPinch
import app.katari.presentation.kit.pointer.isFingerOrWheelScroll
import app.katari.presentation.kit.pointer.touchpadPinch
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import kotlinx.coroutines.delay
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

/**
 * The cover size a [CoverGrid] lays out, which the person can change on the grid itself, shared with a control that
 * changes it too, such as a display panel's slider. Over a grid given it, Ctrl and the wheel, a touchpad pinch or a
 * two-finger pinch step [size] through the sizes, and a pill over the grid names the size and how many covers fit per
 * row while it changes. [columns] is how many covers the grid fits across, 0 until it has laid out.
 */
@Stable
class CoverSizing internal constructor(size: CoverSize, onSizeChange: (CoverSize) -> Unit) {
    var size: CoverSize by mutableStateOf(size)
        internal set

    var columns: Int by mutableIntStateOf(0)
        internal set

    internal var onSizeChange: (CoverSize) -> Unit by mutableStateOf(onSizeChange)

    // The touchpad pinch's scale at its last step, which the next step counts from
    internal var pinchAtStep = 1f

    // Counts the sizes the grid's gestures picked, so the pill shows again after each one
    internal var gestureSteps: Int by mutableIntStateOf(0)
        private set

    /** Changes the size from a control beside the grid, such as a slider. */
    fun resize(to: CoverSize) {
        size = to
        onSizeChange(to)
    }

    internal fun step(by: Int) {
        val next = CoverSize.entries.getOrNull(size.ordinal + by) ?: return
        resize(next)
        gestureSteps++
    }
}

/** A [CoverSizing] at [size], which [onSizeChange] stores when the grid's gestures change it. */
@Composable
fun rememberCoverSizing(size: CoverSize, onSizeChange: (CoverSize) -> Unit): CoverSizing =
    remember { CoverSizing(size, onSizeChange) }.also {
        it.size = size
        it.onSizeChange = onSizeChange
    }

/**
 * Steps [sizing]'s size with Ctrl and the wheel (a notch a step, larger as the wheel turns up), a touchpad pinch or a
 * two-finger pinch, taking those from the grid before it scrolls by them. One finger, and the wheel without Ctrl,
 * still scroll.
 */
@Composable
internal fun Modifier.coverResizing(sizing: CoverSizing): Modifier = this
    .touchpadPinch { pinch, _ -> sizing.pinch(pinch) }
    .pointerInput(sizing) {
        var notches = 0f
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Scroll || !event.keyboardModifiers.isCtrlPressed) continue
                val change = event.changes.first()
                change.consume()
                if (!event.isFingerOrWheelScroll) continue
                // A wheel notch scrolls by 1, a touchpad by fractions of it
                notches -= change.scrollDelta.y
                while (abs(notches) >= 1f) {
                    val step = if (notches > 0) 1 else -1
                    sizing.step(step)
                    notches -= step
                }
            }
        }
    }
    .pointerInput(sizing) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var scale = 1f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } > 1) {
                    scale *= event.calculateZoom()
                    val step = pinchStep(scale)
                    if (step != 0) {
                        sizing.step(step)
                        scale = 1f
                    }
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }

private fun CoverSizing.pinch(pinch: TouchpadPinch) {
    when (pinch) {
        TouchpadPinch.Start -> pinchAtStep = 1f
        is TouchpadPinch.Move -> {
            // A touchpad's pinch scale counts from its start, so the next step counts from the scale of the last
            val step = pinchStep(pinch.scale / pinchAtStep)
            if (step != 0) {
                step(step)
                pinchAtStep = pinch.scale
            }
        }
        TouchpadPinch.End -> Unit
    }
}

/** A step up once fingers have spread by [PINCH_STEP] since the last step, a step down once closed by it, or 0. */
private fun pinchStep(scaleSinceStep: Float): Int = when {
    scaleSinceStep >= PINCH_STEP -> 1
    scaleSinceStep <= 1f / PINCH_STEP -> -1
    else -> 0
}

/** The size and how many covers fit per row, shown for a while after the grid's gestures change it. */
@Composable
internal fun CoverSizePill(sizing: CoverSizing, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(sizing.gestureSteps) {
        if (sizing.gestureSteps == 0) return@LaunchedEffect
        visible = true
        delay(PILL_SHOWN_MILLIS)
        visible = false
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        OverlayPill(style = OverlayStyle.Raised) {
            Text(
                text = pluralStringResource(
                    MR.plurals.cover_size_per_row,
                    sizing.columns,
                    stringResource(sizing.size.labelRes),
                    sizing.columns,
                ),
                role = TextRole.Label,
                modifier = Modifier.padding(
                    horizontal = KatariTheme.spacing.m,
                    vertical = KatariTheme.spacing.s,
                ),
            )
        }
    }
}

// How far apart the fingers move, as a ratio, for each step of size
private const val PINCH_STEP = 1.3f

private const val PILL_SHOWN_MILLIS = 1500L
