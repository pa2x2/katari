package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.list.CollapsibleRowGroupHeader
import mihon.presentation.kit.list.CollapsibleRowGroupSubheader
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Heads one member's chapters in a merged chapter list; tapping it shows or hides them. */
@Composable
internal fun MergedMemberHeader(
    title: String,
    sourceName: String?,
    childCount: Int,
    unreadCount: Int,
    expanded: Boolean,
    entryType: EntryType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollapsibleRowGroupHeader(
        title = title,
        summary = sectionSummary(sourceName, childCount, unreadCount, entryType),
        expanded = expanded,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Heads one group of chapters, such as a volume; a null [title] stands for the chapters in no group. Tapping it shows or
 * hides them and a long press selects them. A [nested] header sits inside its merged member's card.
 */
@Composable
internal fun ChapterGroupHeader(
    title: String?,
    childCount: Int,
    unreadCount: Int,
    expanded: Boolean,
    nested: Boolean,
    entryType: EntryType,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentation = entryType.entryTypePresentation()
    val shownTitle = title ?: stringResource(presentation.otherChildrenLabel)
    val summary = if (childCount == 0) {
        stringResource(presentation.noChildrenYetLabel)
    } else {
        sectionSummary(sourceName = null, childCount, unreadCount, entryType)
    }
    if (nested) {
        CollapsibleRowGroupSubheader(
            title = shownTitle,
            summary = summary,
            expanded = expanded,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
    } else {
        CollapsibleRowGroupHeader(
            title = shownTitle,
            summary = summary,
            expanded = expanded,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun sectionSummary(sourceName: String?, childCount: Int, unreadCount: Int, entryType: EntryType): String {
    val presentation = entryType.entryTypePresentation()
    return listOfNotNull(
        sourceName,
        pluralStringResource(presentation.childCountPlural, childCount, childCount),
        unreadCount.takeIf { it > 0 }?.let {
            stringResource(
                MR.strings.entry_member_unconsumed_count,
                stringResource(presentation.filterUnconsumedLabel),
                it,
            )
        },
    ).joinToString(" · ")
}
