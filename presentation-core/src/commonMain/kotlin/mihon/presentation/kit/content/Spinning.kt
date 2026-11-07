package mihon.presentation.kit.content

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.rotate

/** The way an icon that stands for work under way turns: the way its own arrows point, such as sync's. */
enum class Spin { Clockwise, CounterClockwise }

/** Turns what it modifies a full turn every [TURN_MILLIS] in the [spin] direction, as it is drawn. */
@Composable
internal fun Modifier.spinning(spin: Spin): Modifier {
    val transition = rememberInfiniteTransition(label = "spinning")
    val degrees = transition.animateFloat(
        initialValue = 0f,
        targetValue = when (spin) {
            Spin.Clockwise -> 360f
            Spin.CounterClockwise -> -360f
        },
        animationSpec = infiniteRepeatable(tween(TURN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "spinning-degrees",
    )
    return turningAsDrawn { degrees.value }
}

/**
 * Turns what it modifies by [degrees] about its centre where it is drawn, reading them only there. A turned layer
 * would move its bounds every frame instead, which Compose reports to accessibility services as a content change
 * each time, so a screen never went idle for them for as long as anything on it turned.
 */
internal fun Modifier.turningAsDrawn(degrees: () -> Float): Modifier = drawWithContent {
    rotate(degrees()) { this@drawWithContent.drawContent() }
}

private const val TURN_MILLIS = 1200
