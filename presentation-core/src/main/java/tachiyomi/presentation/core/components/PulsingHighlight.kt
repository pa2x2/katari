package tachiyomi.presentation.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun Modifier.pulsingHighlightBackground(
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
            MaterialTheme.colorScheme.surfaceTint.copy(alpha = .12f)
        } else {
            Color.Transparent
        },
        animationSpec = if (highlightActive) {
            repeatable(
                iterations = iterations,
                animation = tween(durationMillis = 200),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(
                    offsetMillis = 600,
                    offsetType = StartOffsetType.Delay,
                ),
            )
        } else {
            tween(200)
        },
        label = "Pulsing highlight",
    )
    return background(color = highlight)
}
