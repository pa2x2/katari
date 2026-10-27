package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import mihon.presentation.kit.theme.KatariTheme

/**
 * The blocks a schematic is drawn with: a miniature of a layout, such as a library display mode's preview beside its
 * setting. Each part has its own fill and corners, so every schematic in the app reads alike.
 */
enum class SchematicPart {
    /** The schematic's background. */
    Backdrop,

    /** A region of the layout, such as a section of the screen. */
    Panel,

    /** An item, such as an entry's cover or card. */
    Block,

    /** Artwork inside a [Block]. */
    BlockArt,

    /** A line of text inside a [Block]. */
    BlockLine,

    /** A line of text on the [Backdrop]. */
    Line,

    /** Text that stands out, such as a heading. */
    Accent,
}

/** Draws this element as [part] of a schematic. */
@Composable
fun Modifier.schematic(part: SchematicPart): Modifier {
    val colors = KatariTheme.colors
    val shapes = KatariTheme.shapes
    val (fill, shape) = when (part) {
        SchematicPart.Backdrop -> colors.surfaceVariant to shapes.small
        SchematicPart.Panel -> colors.surfaceContainerHighest to shapes.small
        SchematicPart.Block -> colors.primaryContainer to shapes.extraSmall
        SchematicPart.BlockArt -> colors.primary to shapes.extraSmall
        SchematicPart.BlockLine -> colors.onPrimaryContainer to shapes.full
        SchematicPart.Line -> colors.onSurfaceVariant to shapes.full
        SchematicPart.Accent -> colors.primary to shapes.full
    }
    return clip(shape).background(fill)
}
