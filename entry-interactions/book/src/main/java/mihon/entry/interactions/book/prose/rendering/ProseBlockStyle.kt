package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBorderStyle
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle

internal fun BookDocumentRichText.toSpanned(
    inlineTypefaces: Map<String, Typeface>,
): Spanned = text.toSpanned(
    links = links,
    inlineStyles = inlineStyles,
    inlineTypefaces = inlineTypefaces,
)

internal fun String.toSpanned(
    links: List<BookDocumentLink>,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    inlineTypefaces: Map<String, Typeface>,
): Spanned =
    SpannableString(this).apply {
        links.forEach { link ->
            val url = when (val target = link.target) {
                is BookDocumentLinkTarget.Anchor -> "#${target.fragment}"
                is BookDocumentLinkTarget.External -> target.url
            }
            setSpan(
                URLSpan(url),
                link.start,
                link.endExclusive,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }.withInlineDocumentStyles(inlineStyles, inlineTypefaces)

internal fun List<BookDocumentInlineStyleRange>.within(
    start: Int,
    endExclusive: Int,
): List<BookDocumentInlineStyleRange> = mapNotNull { inline ->
    val clippedStart = maxOf(inline.start, start)
    val clippedEnd = minOf(inline.endExclusive, endExclusive)
    if (clippedEnd <= clippedStart) {
        null
    } else {
        inline.copy(
            start = clippedStart - start,
            endExclusive = clippedEnd - start,
        )
    }
}

internal fun BookDocumentStyle.borderModifier(fallbackColor: Color): Modifier {
    val border = border ?: return Modifier
    val color = border.colorArgb?.toComposeColor() ?: fallbackColor.copy(alpha = 0.55f)
    return Modifier.drawBehind {
        val width = border.widthDp.dp.toPx()
        val pathEffect = when (border.style) {
            BookDocumentBorderStyle.SOLID -> null
            BookDocumentBorderStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(width * 4f, width * 3f))
            BookDocumentBorderStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(width, width * 2f))
        }
        drawRect(
            color = color,
            style = Stroke(width = width, pathEffect = pathEffect),
        )
    }
}

internal fun BookDocumentAlignment?.toTextViewAlignment(): Int? = when (this) {
    BookDocumentAlignment.START -> TextView.TEXT_ALIGNMENT_VIEW_START
    BookDocumentAlignment.CENTER -> TextView.TEXT_ALIGNMENT_CENTER
    BookDocumentAlignment.END -> TextView.TEXT_ALIGNMENT_VIEW_END
    null -> null
}

internal fun Long.toComposeColor(): Color = Color(toInt())

internal fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.luminance()
    val secondLuminance = second.luminance()
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

internal fun contrastingTextColor(background: Color): Color =
    if (background.luminance() > 0.45f) Color.Black else Color.White

internal fun Color.toArgbValue(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

internal const val MIN_TEXT_CONTRAST = 4.5f
