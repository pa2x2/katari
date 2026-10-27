package mihon.translation.ui.presentation.language

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole

/**
 * [text] with each translation-direction arrow drawn as an icon centred on the line.
 *
 * The font's arrow glyph sits on the maths axis, which reads as too low next to capitalised language names, so the
 * arrows the language-pair strings use are replaced rather than left to the font.
 */
@Composable
fun TranslationDirectionText(
    text: String,
    modifier: Modifier = Modifier,
    role: TextRole? = null,
    tone: ContentTone = ContentTone.Inherit,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val annotated = remember(text) {
        buildAnnotatedString {
            text.split(DIRECTION_ARROW).forEachIndexed { index, part ->
                if (index > 0) appendInlineContent(DIRECTION_ARROW_ID, DIRECTION_ARROW.toString())
                append(part)
            }
        }
    }
    val arrowTone = arrowTone(role, tone)
    Text(
        text = annotated,
        modifier = modifier,
        role = role,
        tone = tone,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = mapOf(
            DIRECTION_ARROW_ID to InlineTextContent(
                Placeholder(
                    width = DIRECTION_ARROW_SIZE,
                    height = DIRECTION_ARROW_SIZE,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Icon(
                    imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tone = arrowTone,
                )
            },
        ),
    )
}

// Inline content does not inherit the text's colour, so an inherited tone takes the colour the role gives its text.
private fun arrowTone(role: TextRole?, tone: ContentTone): ContentTone = when {
    tone != ContentTone.Inherit -> tone
    role == TextRole.Supporting -> ContentTone.Secondary
    role == TextRole.GroupLabel -> ContentTone.Primary
    else -> ContentTone.Inherit
}

private const val DIRECTION_ARROW_ID = "translation-direction-arrow"
private const val DIRECTION_ARROW = '→'
private val DIRECTION_ARROW_SIZE = 1.15.em
