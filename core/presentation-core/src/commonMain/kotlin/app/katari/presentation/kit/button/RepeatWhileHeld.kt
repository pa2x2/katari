package app.katari.presentation.kit.button

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Acts on a press of the button it modifies rather than on its release, and again and again while the press is held:
 * after a pause, then faster the longer it is held. It takes the press before the button sees it, and reports it to
 * [interactionSource], the button's, so the button still shows it pressed. A click that comes with no press, from the
 * keyboard or an accessibility service, is the button's own and acts once.
 */
@Composable
internal fun Modifier.repeatWhileHeld(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    action: () -> Unit,
): Modifier {
    val latestAction by rememberUpdatedState(action)
    if (!enabled) return this
    return pointerInput(interactionSource) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                down.consume()
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                val repeating = launch {
                    latestAction()
                    delay(FIRST_REPEAT_DELAY_MS)
                    var interval = SLOWEST_REPEAT_MS
                    while (isActive) {
                        latestAction()
                        delay(interval)
                        interval = (interval * REPEAT_SPEEDUP).toLong().coerceAtLeast(FASTEST_REPEAT_MS)
                    }
                }
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                repeating.cancel()
                up?.consume()
                interactionSource.tryEmit(
                    if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                )
            }
        }
    }
}

private const val FIRST_REPEAT_DELAY_MS = 400L
private const val SLOWEST_REPEAT_MS = 150L
private const val FASTEST_REPEAT_MS = 25L
private const val REPEAT_SPEEDUP = 0.85
