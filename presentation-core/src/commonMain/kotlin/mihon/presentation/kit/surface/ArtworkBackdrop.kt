package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * Artwork, such as an entry's cover, laid faintly behind a header: blurred, washed out and fading into the background
 * toward the bottom, so the header's text keeps its normal colours. Unlike [MediaBackdrop] it does not darken what it
 * shows. [image] draws the artwork with the modifier it is given. Size the backdrop to the header through [modifier],
 * such as with matchParentSize, so the artwork never decides the header's height. [bleed] is how far it reaches past
 * the header's sides into the insets beside a list's content, such as a camera cutout, so it runs edge to edge while
 * the header's text stays inset.
 */
@Composable
fun ArtworkBackdrop(
    modifier: Modifier = Modifier,
    bleed: PaddingValues = PaddingValues(),
    image: @Composable (Modifier) -> Unit,
) {
    val fade = Brush.verticalGradient(listOf(Color.Transparent, KatariTheme.colors.background))
    val direction = LocalLayoutDirection.current
    val start = bleed.calculateStartPadding(direction)
    val end = bleed.calculateEndPadding(direction)
    image(
        modifier
            .layout { measurable, constraints ->
                val extra = (start + end).roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = constraints.minWidth + extra,
                        maxWidth = constraints.maxWidth + extra,
                    ),
                )
                layout(placeable.width - extra, placeable.height) { placeable.placeRelative(-start.roundToPx(), 0) }
            }
            .drawWithContent {
                drawContent()
                drawRect(brush = fade)
            }
            .blur(ArtworkBackdropBlur)
            .alpha(ARTWORK_BACKDROP_ALPHA),
    )
}

private val ArtworkBackdropBlur = 16.dp
private const val ARTWORK_BACKDROP_ALPHA = 0.35f
