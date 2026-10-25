package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentShadow
import mihon.entry.interactions.book.document.reader.theme.BookDocumentAuthoredStyling

/**
 * What drawing a run of book text needs to honour the book's own colours: the authored styling, the fitted colour
 * behind the text, and the density its shadows are measured in. Layout measurement leaves it out, since colours and
 * shadows move no line break.
 */
internal class BookDocumentAuthoredText(
    val styling: BookDocumentAuthoredStyling,
    val backdrop: Color,
    private val density: Density,
) {
    /** The text shadow, drawn in [textColor] when the book gives it no colour of its own. */
    fun textShadow(shadow: BookDocumentShadow?, textColor: Color): Shadow? {
        if (shadow == null || !styling.enabled) return null
        return with(density) {
            Shadow(
                color = shadow.colorArgb?.let(styling::fill) ?: textColor.takeOrElse { styling.palette.foreground },
                offset = Offset(shadow.offsetXDp.dp.toPx(), shadow.offsetYDp.dp.toPx()),
                blurRadius = shadow.blurDp.dp.toPx(),
            )
        }
    }
}
