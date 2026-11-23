package app.katari.sync.ui.arrival.change.menu

import androidx.compose.runtime.Composable
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.FilterChange
import app.katari.sync.ui.arrival.change.KeyChange
import app.katari.sync.ui.arrival.change.filterChanges
import app.katari.sync.ui.arrival.change.valueChange
import eu.kanade.presentation.library.settings.label
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The library menu's Sort tab: the sort and its direction, before and after. */
@Composable
internal fun librarySortChanges(changes: List<KeyChange>): List<ChangePart> = changes.mapNotNull { change ->
    val sort = change.of { LibraryPreferences(it).sortingMode } ?: return@mapNotNull null
    valueChange(stringResource(MR.strings.action_sort), sortText(sort.before), sortText(sort.after))
}

@Composable
private fun sortText(sort: LibrarySort): String = if (sort.type == LibrarySort.Type.Random) {
    stringResource(sort.type.label)
} else {
    stringResource(
        MR.strings.sync_arrived_sort,
        stringResource(sort.type.label),
        stringResource(if (sort.isAscending) MR.strings.action_asc else MR.strings.action_desc),
    )
}

/** The library menu's Filter tab, by the labels it shows. */
@Composable
internal fun libraryFilterChanges(changes: List<KeyChange>): List<ChangePart> {
    val filters = listOf(
        MR.strings.label_downloaded to LibraryPreferences::filterDownloaded,
        MR.strings.action_filter_unread to LibraryPreferences::filterUnread,
        MR.strings.label_not_started to LibraryPreferences::filterNotStarted,
        MR.strings.action_filter_bookmarked to LibraryPreferences::filterBookmarked,
        MR.strings.completed to LibraryPreferences::filterCompleted,
        MR.strings.action_filter_interval_custom to LibraryPreferences::filterIntervalCustom,
    )
    val changed = changes.flatMap { change ->
        filters.mapNotNull { (label, filter) ->
            change.of {
                filter(LibraryPreferences(it))
            }?.let { FilterChange(stringResource(label), it.before, it.after) }
        }
    }
    return filterChanges(stringResource(MR.strings.action_filter), changed)
}
