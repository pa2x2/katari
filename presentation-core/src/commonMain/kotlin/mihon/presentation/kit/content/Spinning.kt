package mihon.presentation.kit.content

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

/** Turns what it modifies a full turn every [TURN_MILLIS], for an icon that stands for work under way. */
@Composable
internal fun Modifier.spinning(): Modifier {
    val transition = rememberInfiniteTransition(label = "spinning")
    val degrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(TURN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "spinning-degrees",
    )
    return rotate(degrees)
}

private const val TURN_MILLIS = 1200
