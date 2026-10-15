package mihon.presentation.kit.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.color

/** A run of text in one tone, for [TonedText]. */
@Immutable
data class TonedSpan(val text: String, val tone: ContentTone = ContentTone.Inherit)

/**
 * One text made of [spans] that each keep their own tone, such as a filter summary with included values in the accent
 * colour and excluded ones in the error colour. [separator] joins the spans in the text's own colour. It wraps and
 * shortens as a whole, unlike texts laid side by side.
 */
@Composable
fun TonedText(
    spans: List<TonedSpan>,
    modifier: Modifier = Modifier,
    role: TextRole? = null,
    separator: String = "",
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val colors = spans.map { it.tone.color() }
    val text = buildAnnotatedString {
        spans.forEachIndexed { index, span ->
            if (index > 0) append(separator)
            val color = colors[index]
            if (color.isSpecified) {
                withStyle(SpanStyle(color = color)) { append(span.text) }
            } else {
                append(span.text)
            }
        }
    }
    Text(text = text, modifier = modifier, role = role, maxLines = maxLines, minLines = minLines, overflow = overflow)
}
