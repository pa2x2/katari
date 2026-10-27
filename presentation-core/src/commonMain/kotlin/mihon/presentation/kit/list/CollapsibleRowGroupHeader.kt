package mihon.presentation.kit.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * The header of a row group card that opens and closes, such as one member of a merged chapter list or one volume of a
 * chapter list. [summary] says what the card holds, so it still reads while closed; tapping flips [expanded], and
 * [onLongClick] acts on the whole group, such as selecting its rows.
 */
@Composable
fun CollapsibleRowGroupHeader(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalRowInset provides rowGroupTextInset()) {
        CollapsibleHeaderContent(
            title = title,
            titleRole = TextRole.Label,
            titleTone = ContentTone.Secondary,
            summary = summary,
            expanded = expanded,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
    }
}

/**
 * A header that opens and closes the rows under it inside a [CollapsibleRowGroupHeader]'s card, such as one volume of a
 * merged member's chapters. It is a row of that card, so callers place it in a [RowGroupItem].
 */
@Composable
fun CollapsibleRowGroupSubheader(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    CollapsibleHeaderContent(
        title = title,
        titleRole = TextRole.GroupLabel,
        titleTone = ContentTone.Inherit,
        summary = summary,
        expanded = expanded,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@Composable
private fun CollapsibleHeaderContent(
    title: String,
    titleRole: TextRole,
    titleTone: ContentTone,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = LocalRowInset.current, vertical = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, role = titleRole, tone = titleTone, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = summary, role = TextRole.Supporting)
        }
        Icon(
            imageVector = if (expanded) {
                MaterialSymbols.Rounded.KeyboardArrowUp
            } else {
                MaterialSymbols.Rounded.KeyboardArrowDown
            },
            contentDescription = null,
            tone = ContentTone.Secondary,
        )
    }
}
