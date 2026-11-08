package eu.kanade.presentation.library.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.SortRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.effectiveLibrarySort
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun LibrarySortPage(
    category: Category?,
    viewModel: LibrarySettingsViewModel,
    progressSummaryAvailable: Boolean,
) {
    val globalSort by viewModel.libraryPreferences.sortingMode.collectAsState()
    val currentSort = category.effectiveLibrarySort(globalSort)
    val sortingMode = currentSort.type
    val sortDescending = !currentSort.isAscending

    val options = remember(progressSummaryAvailable) {
        listOfNotNull(
            MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
            (MR.strings.action_sort_total to LibrarySort.Type.TotalChapters)
                .takeIf { progressSummaryAvailable },
            (MR.strings.action_sort_last_read to LibrarySort.Type.LastRead)
                .takeIf { progressSummaryAvailable },
            MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
            (MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount)
                .takeIf { progressSummaryAvailable },
            MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
            MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
            MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
            MR.strings.action_sort_random to LibrarySort.Type.Random,
        )
    }

    RowGroup {
        options.forEach { (titleRes, mode) ->
            row(key = mode) {
                if (mode == LibrarySort.Type.Random) {
                    SettingsRow(
                        title = stringResource(titleRes),
                        leading = {
                            if (sortingMode == LibrarySort.Type.Random) {
                                Icon(
                                    MaterialSymbols.Rounded.Refresh,
                                    contentDescription = null,
                                    tone = ContentTone.Primary,
                                )
                            } else {
                                Spacer(Modifier.size(KatariTheme.sizes.icon))
                            }
                        },
                        onClick = {
                            viewModel.setSort(category, mode, LibrarySort.Direction.Ascending)
                        },
                    )
                } else {
                    SortRow(
                        title = stringResource(titleRes),
                        descending = sortDescending.takeIf { sortingMode == mode },
                        onClick = {
                            val isTogglingDirection = sortingMode == mode
                            val direction = when {
                                isTogglingDirection -> if (sortDescending) {
                                    LibrarySort.Direction.Ascending
                                } else {
                                    LibrarySort.Direction.Descending
                                }
                                else -> if (sortDescending) {
                                    LibrarySort.Direction.Descending
                                } else {
                                    LibrarySort.Direction.Ascending
                                }
                            }
                            viewModel.setSort(category, mode, direction)
                        },
                    )
                }
            }
        }
    }
}
