package mihon.translation.ui.presentation.language

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em

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
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
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
    val arrowColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    Text(
        text = annotated,
        inlineContent = mapOf(
            DIRECTION_ARROW_ID to InlineTextContent(
                Placeholder(
                    width = DIRECTION_ARROW_SIZE,
                    height = DIRECTION_ARROW_SIZE,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = arrowColor,
                )
            },
        ),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private const val DIRECTION_ARROW_ID = "translation-direction-arrow"
private const val DIRECTION_ARROW = '→'
private val DIRECTION_ARROW_SIZE = 1.15.em
