package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/** What artwork from data, such as a cover or an extension's icon, shows while it loads. */
@Composable
fun rememberArtworkPlaceholder(): Painter {
    val color = KatariTheme.colors.placeholder
    return remember(color) { ColorPainter(color) }
}

/** The frame of an entry's cover. */
enum class CoverFrame {
    /** In grids, headers and cards. */
    Cover,

    /** Leading a row, where [Cover]'s corners would eat into the art. */
    Thumbnail,

    /** Left to the cover's container, such as a story page's [storyCoverFrame]. */
    None,
}

/** What shows inside a cover's frame where the cover itself doesn't. */
enum class CoverBacking {
    None,

    /** Around a cover fit inside its frame rather than cropped to it. */
    Letterbox,

    /** In place of a cover that is loading. */
    Placeholder,

    /** In a slot that has no cover, such as a match not chosen yet. */
    Empty,
}

/**
 * Clips this element to a cover's [frame], for a cover or anything laid over or standing in for one, with [backing]
 * behind it. A [raised] cover casts a shadow, such as the cover at the top of an entry.
 */
@Composable
fun Modifier.coverFrame(
    frame: CoverFrame = CoverFrame.Cover,
    backing: CoverBacking = CoverBacking.None,
    raised: Boolean = false,
): Modifier {
    val shapes = KatariTheme.shapes
    val colors = KatariTheme.colors
    val shape = when (frame) {
        CoverFrame.Cover -> shapes.cover
        CoverFrame.Thumbnail -> shapes.coverThumbnail
        CoverFrame.None -> shapes.none
    }
    val fill = when (backing) {
        CoverBacking.None -> null
        CoverBacking.Letterbox -> colors.surfaceContainerHigh
        CoverBacking.Placeholder -> colors.placeholder
        CoverBacking.Empty -> colors.surfaceContainerHighest
    }
    return then(if (raised) Modifier.shadow(RaisedCoverElevation, shape) else Modifier)
        .clip(shape)
        .then(if (fill != null) Modifier.background(fill) else Modifier)
}

/** Stands in for text or a detail that is still loading, sized by the caller. */
@Composable
fun Modifier.skeleton(): Modifier = clip(KatariTheme.shapes.extraSmall).background(KatariTheme.colors.placeholder)

private val RaisedCoverElevation = 8.dp
