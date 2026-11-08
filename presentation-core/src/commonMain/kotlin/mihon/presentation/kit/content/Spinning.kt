package mihon.presentation.kit.content

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import mihon.presentation.kit.theme.ContentTone

/** The way an icon that stands for work under way turns: the way its own arrows point, such as sync's. */
enum class Spin { Clockwise, CounterClockwise }

/**
 * [icon], turned by [spin] while it is set. With [arrows], only they turn and [icon] stays still: they are drawn over it
 * at its size and turn about the centre of their own drawing, such as sync's arrows inside its cloud.
 */
@Composable
internal fun SpinningIcon(
    icon: ImageVector,
    contentDescription: String?,
    spin: Spin?,
    arrows: ImageVector?,
    tone: ContentTone,
) {
    if (arrows == null) {
        Icon(icon, contentDescription, if (spin != null) Modifier.spinning(spin) else Modifier, tone = tone)
        return
    }
    Box {
        Icon(icon, contentDescription, tone = tone)
        val pivot = remember(arrows) { arrows.drawnCentre() }
        Icon(arrows, null, if (spin != null) Modifier.spinning(spin, pivot) else Modifier, tone = tone)
    }
}

/**
 * Turns what it modifies a full turn every [TURN_MILLIS] in the [spin] direction, as it is drawn, about [pivot], a
 * fraction of its size: its centre unless given.
 */
@Composable
internal fun Modifier.spinning(spin: Spin, pivot: Offset = CENTRE): Modifier {
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
    return turningAsDrawn(pivot) { degrees.value }
}

/**
 * Turns what it modifies by [degrees] about [pivot], a fraction of its size, where it is drawn, reading them only
 * there. A turned layer would move its bounds every frame instead, which Compose reports to accessibility services as a
 * content change each time, so a screen never went idle for them for as long as anything on it turned.
 */
internal fun Modifier.turningAsDrawn(pivot: Offset = CENTRE, degrees: () -> Float): Modifier = drawWithContent {
    rotate(degrees(), Offset(size.width * pivot.x, size.height * pivot.y)) { this@drawWithContent.drawContent() }
}

/**
 * The centre of what this vector draws, as a fraction of its viewport. Groups' transforms are left out: the icon sets
 * are drawn without any.
 */
private fun ImageVector.drawnCentre(): Offset {
    val drawn = root.drawnBounds() ?: return CENTRE
    return Offset(drawn.center.x / viewportWidth, drawn.center.y / viewportHeight)
}

private fun VectorGroup.drawnBounds(): Rect? = mapNotNull { node ->
    when (node) {
        is VectorGroup -> node.drawnBounds()
        is VectorPath -> node.pathData.toPath().getBounds()
    }
}.reduceOrNull(Rect::spanning)

private fun Rect.spanning(other: Rect) = Rect(
    minOf(left, other.left),
    minOf(top, other.top),
    maxOf(right, other.right),
    maxOf(bottom, other.bottom),
)

private val CENTRE = Offset(0.5f, 0.5f)

private const val TURN_MILLIS = 1200
