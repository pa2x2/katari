package mihon.presentation.kit.content

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp

/** Frames a cover of [width] on a story page: corners and a shadow in proportion to it, so every cover looks alike. */
fun Modifier.storyCoverFrame(width: Dp): Modifier {
    val shape = StoryMetrics.coverShape(width)
    return shadow(elevation = StoryMetrics.coverShadow(width), shape = shape).clip(shape)
}
