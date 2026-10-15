package mihon.presentation.kit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import mihon.icons.materialsymbols.roundedfilled.Circle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.lineHeight
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.color
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.ui.semantics.selected as selectedState

/**
 * A chapter, episode or table-of-contents entry: its title, marked when unread or bookmarked, over one line of details
 * joined by dots, with an optional [trailing] control such as a download button. Read entries are dimmed.
 *
 * Chapter lists on screens run edge to edge, so the row takes the content inset there; inside a sheet it takes the
 * sheet's row inset. [depth] indents the nested entries of a table of contents.
 *
 * @param read null when the entry has no read state of its own, which shows neither the unread mark nor dimming.
 * @param progress where reading stopped, dimmed further than the other details since it is only a hint.
 */
@Composable
fun ChapterRow(
    title: String,
    read: Boolean?,
    bookmark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    date: String? = null,
    duration: String? = null,
    progress: String? = null,
    scanlator: String? = null,
    unreadLabel: String = stringResource(MR.strings.action_filter_unseen),
    selected: Boolean = false,
    depth: Int = 0,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    val inset = if (LocalRowDensity.current == RowDensity.Compact) LocalRowInset.current else spacing.contentInset
    Row(
        modifier = modifier
            .then(if (selected) Modifier.background(KatariTheme.colors.selected) else Modifier)
            .semantics { selectedState = selected }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = inset + spacing.l * depth, top = spacing.m, end = inset, bottom = spacing.m),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChapterMarks(unread = read == false, bookmark = bookmark, unreadLabel = unreadLabel)
                Text(
                    text = title,
                    role = TextRole.Body,
                    tone = if (read == true) ContentTone.Disabled else ContentTone.Inherit,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ChapterDetails(
                read = read == true,
                date = date,
                duration = duration,
                progress = progress,
                scanlator = scanlator,
            )
        }
        trailing?.invoke()
    }
}

/**
 * The marks that lead a chapter's title: a dot while it is unread and a bookmark when it is bookmarked. Place them in a
 * row before the text they lead; [role] is that text's role, which the bookmark matches in height.
 */
@Composable
fun ChapterMarks(
    unread: Boolean,
    bookmark: Boolean,
    unreadLabel: String = stringResource(MR.strings.action_filter_unseen),
    role: TextRole = TextRole.Body,
) {
    if (unread) {
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.Circle,
            contentDescription = unreadLabel,
            modifier = Modifier.size(UnreadMarkSize),
            tone = ContentTone.Primary,
        )
    }
    if (bookmark) {
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.Bookmark,
            contentDescription = stringResource(MR.strings.action_filter_bookmarked),
            modifier = Modifier.size(role.lineHeight - KatariTheme.spacing.xxs),
            tone = ContentTone.Primary,
        )
    }
}

@Composable
private fun ChapterDetails(
    read: Boolean,
    date: String?,
    duration: String?,
    progress: String?,
    scanlator: String?,
) {
    val parts = listOfNotNull(
        date?.let { it to false },
        duration?.let { it to false },
        progress?.let { it to true },
        scanlator?.let { it to false },
    )
    if (parts.isEmpty()) return
    val dimmed = ContentTone.Disabled.color()
    val text = buildAnnotatedString {
        parts.forEachIndexed { index, (part, isHint) ->
            if (index > 0) append(DETAIL_SEPARATOR)
            if (isHint) withStyle(SpanStyle(color = dimmed)) { append(part) } else append(part)
        }
    }
    Text(
        text = text,
        role = TextRole.Supporting,
        tone = if (read) ContentTone.Disabled else ContentTone.Inherit,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private val UnreadMarkSize = 8.dp
private const val DETAIL_SEPARATOR = " • "
