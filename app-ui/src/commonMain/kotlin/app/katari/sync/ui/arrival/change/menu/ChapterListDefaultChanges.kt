package app.katari.sync.ui.arrival.change.menu

import androidx.compose.runtime.Composable
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.FilterChange
import app.katari.sync.ui.arrival.change.KeyChange
import app.katari.sync.ui.arrival.change.filterChanges
import app.katari.sync.ui.arrival.change.switchChange
import app.katari.sync.ui.arrival.change.valueChange
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The chapter list defaults a title's chapter menu saves with Set as default, by that menu's labels. */
@Composable
internal fun chapterListDefaultChanges(changes: List<KeyChange>): List<ChangePart> {
    val filters = listOf(
        ChapterFilter(MR.strings.label_downloaded, LibraryPreferences::filterChapterByDownloaded) {
            Entry.chapterFilter(it, Entry.CHAPTER_SHOW_DOWNLOADED, Entry.CHAPTER_SHOW_NOT_DOWNLOADED)
        },
        ChapterFilter(MR.strings.action_filter_unread, LibraryPreferences::filterChapterByRead) {
            Entry.chapterFilter(it, Entry.CHAPTER_SHOW_UNREAD, Entry.CHAPTER_SHOW_READ)
        },
        ChapterFilter(MR.strings.action_filter_bookmarked, LibraryPreferences::filterChapterByBookmarked) {
            Entry.chapterFilter(it, Entry.CHAPTER_SHOW_BOOKMARKED, Entry.CHAPTER_SHOW_NOT_BOOKMARKED)
        },
        ChapterFilter(MR.strings.chapter_kind_filler, LibraryPreferences::filterChapterByFiller) {
            Entry.chapterFilter(it, Entry.CHAPTER_SHOW_FILLER, Entry.CHAPTER_SHOW_NOT_FILLER)
        },
    )
    val filtered = changes.flatMap { change ->
        filters.mapNotNull { filter ->
            change.of { filter.preference(LibraryPreferences(it)) }?.let {
                FilterChange(stringResource(filter.label), filter.state(it.before), filter.state(it.after))
            }
        }
    }
    val chosen = changes.mapNotNull { change ->
        change.of { LibraryPreferences(it).sortChapterBySourceOrNumber }?.let {
            return@mapNotNull valueChange(
                stringResource(MR.strings.action_sort),
                sortText(it.before),
                sortText(it.after),
            )
        }
        change.of { LibraryPreferences(it).sortChapterByAscendingOrDescending }?.let {
            return@mapNotNull valueChange(
                stringResource(MR.strings.action_sort),
                directionText(it.before),
                directionText(it.after),
            )
        }
        change.of { LibraryPreferences(it).displayChapterByNameOrNumber }?.let {
            return@mapNotNull valueChange(
                stringResource(MR.strings.action_display),
                displayText(it.before),
                displayText(it.after),
            )
        }
        change.of { LibraryPreferences(it).showChapterPreviews }?.let {
            return@mapNotNull switchChange(
                stringResource(MR.strings.chapter_previews_and_summaries),
                it.before,
                it.after,
            )
        }
        null
    }
    return filterChanges(stringResource(MR.strings.action_filter), filtered) + chosen
}

private class ChapterFilter(
    val label: StringResource,
    val preference: (LibraryPreferences) -> Preference<Long>,
    val state: (Long) -> TriState,
)

@Composable
private fun sortText(flags: Long): String = stringResource(
    when (flags and Entry.CHAPTER_SORTING_MASK) {
        Entry.CHAPTER_SORTING_NUMBER -> MR.strings.sort_by_number
        Entry.CHAPTER_SORTING_UPLOAD_DATE -> MR.strings.sort_by_upload_date
        Entry.CHAPTER_SORTING_ALPHABET -> MR.strings.action_sort_alpha
        else -> MR.strings.sort_by_source
    },
)

@Composable
private fun directionText(flags: Long): String = stringResource(
    if (flags and Entry.CHAPTER_SORT_DIR_MASK ==
        Entry.CHAPTER_SORT_ASC
    ) {
        MR.strings.action_asc
    } else {
        MR.strings.action_desc
    },
)

@Composable
private fun displayText(flags: Long): String = stringResource(
    if (flags and Entry.CHAPTER_DISPLAY_MASK == Entry.CHAPTER_DISPLAY_NUMBER) {
        MR.strings.show_chapter_number
    } else {
        MR.strings.show_title
    },
)
