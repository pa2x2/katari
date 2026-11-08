package mihon.presentation.kit.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.TonedSpan
import mihon.presentation.kit.content.TonedText
import mihon.presentation.kit.pointer.clickCursor
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/*
 * Headers of groups that open and close. Both take an optional leading [icon], [error] for a group with a problem in
 * it, such as filters a source can't apply, and [actions] before the arrow, such as resetting the group. [opensPage]
 * makes the header open the group on a page of its own instead, with an arrow pointing there. The summary says what
 * the group holds, so it still reads while closed; spans of it can take their own tone, such as excluded values.
 */

/**
 * The header of a row group card that opens and closes, such as one member of a merged chapter list or one volume of a
 * chapter list. Tapping flips [expanded], and [onLongClick] acts on the whole group, such as selecting its rows.
 */
@Composable
fun CollapsibleRowGroupHeader(
    title: String,
    summary: List<TonedSpan>,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    error: Boolean = false,
    opensPage: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalRowInset provides rowGroupTextInset()) {
        CollapsibleHeaderContent(
            title = title,
            titleRole = TextRole.Label,
            titleTone = if (error) ContentTone.Error else ContentTone.Secondary,
            summary = summary,
            expanded = expanded,
            opensPage = opensPage,
            icon = icon,
            actions = actions,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
    }
}

@Composable
fun CollapsibleRowGroupHeader(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    error: Boolean = false,
    opensPage: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    CollapsibleRowGroupHeader(
        title = title,
        summary = listOf(TonedSpan(summary)),
        expanded = expanded,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        icon = icon,
        error = error,
        opensPage = opensPage,
        actions = actions,
    )
}

/**
 * A header that opens and closes the rows under it inside a row group card, such as one volume of a merged member's
 * chapters or one group of a source's filters. It is a row of that card, so callers place it in a [RowGroupItem].
 */
@Composable
fun CollapsibleRowGroupSubheader(
    title: String,
    summary: List<TonedSpan>,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    error: Boolean = false,
    opensPage: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    CollapsibleHeaderContent(
        title = title,
        titleRole = TextRole.GroupLabel,
        titleTone = if (error) ContentTone.Error else ContentTone.Inherit,
        summary = summary,
        expanded = expanded,
        opensPage = opensPage,
        icon = icon,
        actions = actions,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@Composable
fun CollapsibleRowGroupSubheader(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    error: Boolean = false,
    opensPage: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    CollapsibleRowGroupSubheader(
        title = title,
        summary = listOf(TonedSpan(summary)),
        expanded = expanded,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        icon = icon,
        error = error,
        opensPage = opensPage,
        actions = actions,
    )
}

@Composable
private fun CollapsibleHeaderContent(
    title: String,
    titleRole: TextRole,
    titleTone: ContentTone,
    summary: List<TonedSpan>,
    expanded: Boolean,
    opensPage: Boolean,
    icon: ImageVector?,
    actions: (@Composable RowScope.() -> Unit)?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickCursor()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = LocalRowInset.current, vertical = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                tone = if (titleTone == ContentTone.Error) ContentTone.Error else ContentTone.Secondary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, role = titleRole, tone = titleTone, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (summary.isNotEmpty()) {
                TonedText(
                    spans = summary,
                    role = TextRole.Supporting,
                    separator = " · ",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions?.invoke(this)
        Icon(
            imageVector = when {
                opensPage -> MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight
                expanded -> MaterialSymbols.Rounded.KeyboardArrowUp
                else -> MaterialSymbols.Rounded.KeyboardArrowDown
            },
            contentDescription = null,
            tone = ContentTone.Secondary,
        )
    }
}
