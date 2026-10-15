package mihon.presentation.kit.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * Artwork, such as an entry's cover, laid faintly behind a header: blurred, washed out and fading into the background
 * toward the bottom, so the header's text keeps its normal colours. Unlike [MediaBackdrop] it does not darken what it
 * shows. [image] draws the artwork with the modifier it is given. Size the backdrop to the header through [modifier],
 * such as with matchParentSize, so the artwork never decides the header's height.
 */
@Composable
fun ArtworkBackdrop(
    modifier: Modifier = Modifier,
    image: @Composable (Modifier) -> Unit,
) {
    val fade = Brush.verticalGradient(listOf(Color.Transparent, KatariTheme.colors.background))
    image(
        modifier
            .drawWithContent {
                drawContent()
                drawRect(brush = fade)
            }
            .blur(ArtworkBackdropBlur)
            .alpha(ARTWORK_BACKDROP_ALPHA),
    )
}

private val ArtworkBackdropBlur = 4.dp
private const val ARTWORK_BACKDROP_ALPHA = 0.2f
