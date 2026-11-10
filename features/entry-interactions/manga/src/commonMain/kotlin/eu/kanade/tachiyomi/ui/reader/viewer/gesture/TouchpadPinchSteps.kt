package eu.kanade.tachiyomi.ui.reader.viewer.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import app.katari.presentation.kit.pointer.TouchpadPinch
import app.katari.presentation.kit.pointer.touchpadPinch

/**
 * A touchpad pinch over the viewer, told step by step as a two-finger touch is, so both take the same zoom and pan
 * code. [onStart] gets the pointer, which a touchpad pinch doesn't move, and says whether to follow the pinch. Each
 * [onStep] gives the zoom factor since the last step, the fingers' middle in the viewer to zoom about, and how far
 * that middle moved since the last step.
 */
@Composable
internal fun Modifier.touchpadPinchSteps(
    onStart: (pointer: Offset) -> Boolean,
    onStep: (factor: Float, focus: Offset, pan: Offset) -> Unit,
): Modifier {
    val steps = remember { PinchSteps() }
    return touchpadPinch { pinch, pointer -> steps.onPinch(pinch, pointer, onStart, onStep) }
}

// The kit reports a pinch's scale and pan since it started; touch gestures and the viewers' zooms work in steps
private class PinchSteps {
    private var start: Offset? = null
    private var scale = 1f
    private var pan = Offset.Zero

    fun onPinch(
        pinch: TouchpadPinch,
        pointer: Offset,
        onStart: (Offset) -> Boolean,
        onStep: (factor: Float, focus: Offset, pan: Offset) -> Unit,
    ) {
        when (pinch) {
            TouchpadPinch.Start -> {
                start = pointer.takeIf(onStart)
                scale = 1f
                pan = Offset.Zero
            }
            is TouchpadPinch.Move -> {
                val start = start ?: return
                if (pinch.scale <= 0f) return
                onStep(pinch.scale / scale, start + pinch.pan, pinch.pan - pan)
                scale = pinch.scale
                pan = pinch.pan
            }
            TouchpadPinch.End -> start = null
        }
    }
}
