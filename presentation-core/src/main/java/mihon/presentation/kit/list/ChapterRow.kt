package mihon.presentation.kit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import mihon.icons.materialsymbols.roundedfilled.Circle
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.lineHeight
import mihon.presentation.kit.surface.selectionFill
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
 * A source may describe a chapter further: a [preview] still leads the row, a [kind] tag such as "Filler" leads the
 * details, and two lines of its [summary] follow them. Tapping the still calls [onPreviewClick], such as to open the
 * chapter's [ChapterSheet][mihon.presentation.kit.sheet.ChapterSheet].
 *
 * Chapter lists on screens run edge to edge, so the row takes the content inset there; inside a sheet it takes the
 * sheet's row inset. [depth] indents the nested entries of a table of contents.
 *
 * @param read null when the entry has no read state of its own, which shows neither the unread mark nor dimming.
 * @param progress where reading stopped, dimmed further than the other details since it is only a hint.
 * @param preview draws the still with the modifier it is given, which sizes and clips it.
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
    kind: String? = null,
    summary: String? = null,
    preview: (@Composable (Modifier) -> Unit)? = null,
    onPreviewClick: (() -> Unit)? = null,
    unreadLabel: String = stringResource(MR.strings.action_filter_unseen),
    selected: Boolean = false,
    depth: Int = 0,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    val inset = if (LocalRowDensity.current == RowDensity.Compact) LocalRowInset.current else spacing.contentInset
    TrailingControlsLayout(
        modifier = modifier
            .selectionFill(selected)
            .semantics { selectedState = selected }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = inset + spacing.l * depth, top = spacing.m, end = inset, bottom = spacing.m),
        leading = {
            if (preview != null) {
                ChapterPreview(
                    preview = preview,
                    onClick = onPreviewClick,
                    modifier = Modifier
                        .padding(end = spacing.xs)
                        .width(ChapterRowPreviewWidth),
                )
            }
        },
        words = {
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
                kind = kind,
                date = date,
                duration = duration,
                progress = progress,
                scanlator = scanlator,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    role = TextRole.Supporting,
                    tone = if (read == true) ContentTone.Disabled else ContentTone.Secondary,
                    maxLines = SUMMARY_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = { if (trailing != null) RowTrailing(trailing) },
        leadingGap = spacing.s,
        trailingGap = spacing.s,
        lineGap = spacing.xs,
    )
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

/**
 * A chapter's still at [modifier]'s width, framed 16:9 with a placeholder behind it while it loads or when it fails.
 */
@Composable
internal fun ChapterPreview(
    preview: @Composable (Modifier) -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(CHAPTER_PREVIEW_RATIO)
            .clip(KatariTheme.shapes.coverThumbnail)
            .background(KatariTheme.colors.placeholder)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        preview(Modifier.matchParentSize())
    }
}

/** The details line of a chapter: its [kind] as a tag, then the rest joined by dots. */
@Composable
internal fun ChapterDetails(
    read: Boolean,
    kind: String?,
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
    if (parts.isEmpty() && kind == null) return
    val dimmed = ContentTone.Disabled.color()
    val text = buildAnnotatedString {
        parts.forEachIndexed { index, (part, isHint) ->
            if (index > 0) append(DETAIL_SEPARATOR)
            if (isHint) withStyle(SpanStyle(color = dimmed)) { append(part) } else append(part)
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (kind != null) Badge(text = kind, tone = BadgeTone.Tertiary)
        Text(
            text = text,
            role = TextRole.Supporting,
            tone = if (read) ContentTone.Disabled else ContentTone.Inherit,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val UnreadMarkSize = 8.dp
private val ChapterRowPreviewWidth = 112.dp
private const val CHAPTER_PREVIEW_RATIO = 16f / 9f
private const val SUMMARY_MAX_LINES = 2
private const val DETAIL_SEPARATOR = " • "
