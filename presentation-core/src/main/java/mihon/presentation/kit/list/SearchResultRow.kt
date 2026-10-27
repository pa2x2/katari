package mihon.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * One place a search found, shown as the [excerpt] of text around it with the matched characters, from [matchStart]
 * up to [matchEnd], picked out.
 */
@Composable
fun SearchResultRow(
    excerpt: String,
    matchStart: Int,
    matchEnd: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasis = KatariTheme.colors.primary
    val text = remember(excerpt, matchStart, matchEnd, emphasis) {
        buildAnnotatedString {
            append(excerpt)
            val start = matchStart.coerceIn(0, excerpt.length)
            val end = matchEnd.coerceIn(start, excerpt.length)
            addStyle(SpanStyle(color = emphasis, fontWeight = FontWeight.Bold), start, end)
        }
    }
    RowFrame(
        modifier = modifier,
        enabled = true,
        role = Role.Button,
        onClick = onClick,
        onLongClick = null,
    ) {
        Text(text, role = TextRole.Body, maxLines = EXCERPT_LINES, overflow = TextOverflow.Ellipsis)
    }
}

private const val EXCERPT_LINES = 3
