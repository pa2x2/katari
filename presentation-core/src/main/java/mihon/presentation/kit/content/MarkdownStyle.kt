package mihon.presentation.kit.content

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import mihon.presentation.kit.theme.KatariTheme

/**
 * How rendered Markdown looks, for the app's Markdown renderer to hand to its library: the six heading levels,
 * paragraphs, code, quotes and links, and the fills behind code and tables.
 *
 * @param headings styles of heading levels 1 to 6, in order.
 * @param inlineImageSize side of the mark that stands in for an inline image.
 */
@Immutable
class MarkdownStyle internal constructor(
    val headings: List<TextStyle>,
    val body: TextStyle,
    val code: TextStyle,
    val quote: TextStyle,
    val link: SpanStyle,
    val textColor: Color,
    val codeBackground: Color,
    val tableBackground: Color,
    val dividerColor: Color,
    val inlineImageSize: TextUnit,
)

/** [secondary] renders body text in the secondary content colour, for text that supports a screen. */
@Composable
@ReadOnlyComposable
fun markdownStyle(secondary: Boolean = false): MarkdownStyle {
    val typography = MaterialTheme.typography
    val colors = KatariTheme.colors
    val body = typography.bodyMedium
    return MarkdownStyle(
        headings = listOf(
            typography.headlineMedium,
            typography.headlineSmall,
            typography.titleLarge,
            typography.titleMedium,
            typography.titleSmall,
            typography.bodyLarge,
        ),
        body = body,
        code = body.copy(fontFamily = FontFamily.Monospace),
        quote = body.plus(SpanStyle(fontStyle = FontStyle.Italic)),
        link = SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold),
        textColor = if (secondary) colors.onSurfaceVariant else colors.onSurface,
        codeBackground = colors.onSurface.copy(alpha = CODE_BACKGROUND_ALPHA),
        tableBackground = colors.onSurface.copy(alpha = TABLE_BACKGROUND_ALPHA),
        dividerColor = colors.outlineVariant,
        inlineImageSize = body.fontSize * INLINE_IMAGE_SCALE,
    )
}

private const val CODE_BACKGROUND_ALPHA = 0.1f
private const val TABLE_BACKGROUND_ALPHA = 0.05f
private const val INLINE_IMAGE_SCALE = 1.25
