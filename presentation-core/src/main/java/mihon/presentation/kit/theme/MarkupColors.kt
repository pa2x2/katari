package mihon.presentation.kit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colours for marks drawn over a page or image, read through KatariTheme.markup: the area being outlined, the shade
 * around it, and outlined regions with the badges that number them.
 */
@Immutable
class MarkupColors internal constructor(
    /** Dims the image around the area being outlined. */
    val shade: Color,
    /** Fills the area being outlined; the image under it stays visible. */
    val fill: Color,
    /** Outlines of areas and regions, and the badges numbering them. */
    val mark: Color,
    /** Text on a badge. */
    val onMark: Color,
)
