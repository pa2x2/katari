package mihon.presentation.kit.surface

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import mihon.presentation.kit.theme.KatariTheme

/**
 * Pulses the background a few times to point the user at an item, such as a setting opened from search. A new
 * non-null [trigger] starts the pulse; it fades after [highlightDurationMillis]. [visible] false hides it while the
 * item shows another background, such as its selected state.
 */
@Composable
fun Modifier.pulsingHighlight(
    trigger: Any?,
    visible: Boolean = true,
    iterations: Int = 5,
    highlightDurationMillis: Long = 3_000L,
): Modifier {
    var highlightActive by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        highlightActive = trigger != null
        if (highlightActive) {
            delay(highlightDurationMillis)
            highlightActive = false
        }
    }
    val highlight by animateColorAsState(
        targetValue = if (highlightActive && visible) {
            KatariTheme.colors.surfaceTint.copy(alpha = HIGHLIGHT_ALPHA)
        } else {
            Color.Transparent
        },
        animationSpec = if (highlightActive) {
            repeatable(
                iterations = iterations,
                animation = tween(durationMillis = PULSE_MILLIS),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(
                    offsetMillis = START_DELAY_MILLIS,
                    offsetType = StartOffsetType.Delay,
                ),
            )
        } else {
            tween(PULSE_MILLIS)
        },
        label = "Pulsing highlight",
    )
    return background(color = highlight)
}

private const val HIGHLIGHT_ALPHA = 0.12f
private const val PULSE_MILLIS = 200
private const val START_DELAY_MILLIS = 600
