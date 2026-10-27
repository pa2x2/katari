package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariShapes
import mihon.presentation.kit.theme.KatariSizes
import mihon.presentation.kit.theme.KatariTheme

/** Where a logo sits, which sets its size and how round its corners are. */
enum class LogoSize(internal val side: (KatariSizes) -> Dp, internal val shape: (KatariShapes) -> Shape) {
    /** Beside a line of text, such as a feed's tab. */
    Inline({ it.iconSmall }, { it.extraSmall }),

    /** In place of an icon, such as in a menu item or chip. */
    Icon({ it.icon }, { it.extraSmall }),

    /** Leading a row in a sheet, where [Row] would crowd it. */
    Compact({ it.iconLarge }, { it.extraSmall }),

    /** Leading a list row. */
    Row({ LogoRowSide }, { it.small }),

    /** A logo that is itself the control, such as a tracker's. */
    Large({ it.touchTarget }, { it.medium }),

    /** At the top of a details screen. */
    Header({ it.artworkLarge }, { it.medium }),
}

/**
 * A logo from data, such as a source's or extension's icon, on the rounded tile every logo shares so differently drawn
 * logos line up. [content] draws the image filling the tile.
 */
@Composable
fun LogoFrame(
    size: LogoSize,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size.side(KatariTheme.sizes))
            .clip(size.shape(KatariTheme.shapes))
            .background(KatariTheme.colors.surfaceContainer),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private val LogoRowSide = 40.dp
