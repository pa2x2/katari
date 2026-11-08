package mihon.presentation.kit.content

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.color
import mihon.presentation.kit.theme.defaultColor
import mihon.presentation.kit.theme.style

/**
 * Text in one of the kit's roles. A null [role] takes the style of the enclosing kit component, such as a button's
 * label style, so text inside components never sets its own.
 *
 * @param shrinkTo keeps the text on one line, shrinking it from [role]'s size down to this role's when it does not
 * fit, such as a large figure that must stay whole. Needs a [role].
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    role: TextRole? = null,
    tone: ContentTone = ContentTone.Inherit,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    shrinkTo: TextRole? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    val autoSize = if (role != null && shrinkTo != null) {
        val typography = MaterialTheme.typography
        TextAutoSize.StepBased(
            minFontSize = shrinkTo.style(typography).fontSize,
            maxFontSize = role.style(typography).fontSize,
        )
    } else {
        null
    }
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = tone.color(fallback = role?.defaultColor(KatariTheme.colors) ?: Color.Unspecified),
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = if (autoSize != null) 1 else maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        autoSize = autoSize,
        style = resolveStyle(role),
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    role: TextRole? = null,
    tone: ContentTone = ContentTone.Inherit,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = tone.color(fallback = role?.defaultColor(KatariTheme.colors) ?: Color.Unspecified),
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = resolveStyle(role),
    )
}

/**
 * The role's style for text measured and drawn on a Canvas, such as labels in a chart; composed text uses [Text]. It
 * carries no colour: pass one from KatariTheme.colors when drawing.
 */
val TextRole.drawingStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = style(MaterialTheme.typography)

/** How tall one line of text in this role is, for areas sized by how many lines they show. */
val TextRole.lineHeight: Dp
    @Composable
    @ReadOnlyComposable
    get() = with(LocalDensity.current) { style(MaterialTheme.typography).lineHeight.toDp() }

/** Makes text without its own role inside [content] use [role]'s style, for a surface whose text is mostly of one kind. */
@Composable
fun ProvideTextRole(role: TextRole, content: @Composable () -> Unit) {
    ProvideTextStyle(role.style(MaterialTheme.typography), content)
}

@Composable
private fun resolveStyle(role: TextRole?): TextStyle = role?.style(MaterialTheme.typography) ?: LocalTextStyle.current
