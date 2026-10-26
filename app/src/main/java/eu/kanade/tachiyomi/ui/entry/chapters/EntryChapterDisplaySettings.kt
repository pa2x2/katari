package eu.kanade.tachiyomi.ui.entry.chapters

import android.content.Context
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import eu.kanade.tachiyomi.ui.entry.members.EntryMembers
import kotlinx.coroutines.CoroutineScope
import mihon.entry.interactions.child.EntryScanlatorFilterFeature
import mihon.entry.interactions.child.EntryScanlatorFilterScope
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.presentation.kit.feedback.SnackbarHostState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.interactor.SetEntryChapterFlags
import tachiyomi.domain.entry.interactor.SetEntryDefaultChapterFlags
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.context.stringResource

/**
 * How an entry screen filters, sorts and shows its chapters. Each setting is stored on every member the screen covers,
 * so a merged entry's members keep showing their chapters alike.
 */
@AssistedInject
class EntryChapterDisplaySettings(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val context: Context,
    @Assisted private val snackbarHostState: SnackbarHostState,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val members: EntryMembers,
    private val setEntryChapterFlags: SetEntryChapterFlags,
    private val setEntryDefaultChapterFlags: SetEntryDefaultChapterFlags,
    private val libraryPreferences: LibraryPreferences,
    private val entryScanlatorFilterFeature: EntryScanlatorFilterFeature,
    private val entryTypePresentationFeature: EntryTypePresentationFeature,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            context: Context,
            snackbarHostState: SnackbarHostState,
            state: EntryStateAccess,
            members: EntryMembers,
        ): EntryChapterDisplaySettings
    }

    fun setUnreadFilter(filter: TriState) {
        setFlags { flags ->
            flags.setUnreadFilter(
                when (filter) {
                    TriState.DISABLED -> Entry.SHOW_ALL
                    TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_UNREAD
                    TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_READ
                },
            )
        }
    }

    fun setDownloadedFilter(filter: TriState) {
        setFlags { flags ->
            flags.setDownloadedFilter(
                when (filter) {
                    TriState.DISABLED -> Entry.SHOW_ALL
                    TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_DOWNLOADED
                    TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_NOT_DOWNLOADED
                },
            )
        }
    }

    fun setBookmarkedFilter(filter: TriState) {
        setFlags { flags ->
            flags.setBookmarkFilter(
                when (filter) {
                    TriState.DISABLED -> Entry.SHOW_ALL
                    TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_BOOKMARKED
                    TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_NOT_BOOKMARKED
                },
            )
        }
    }

    fun setFillerFilter(filter: TriState) {
        setFlags { flags ->
            flags.setFillerFilter(
                when (filter) {
                    TriState.DISABLED -> Entry.SHOW_ALL
                    TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_FILLER
                    TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_NOT_FILLER
                },
            )
        }
    }

    fun setShowChapterPreviews(show: Boolean) {
        setFlags { flags -> flags.setShowChapterPreviews(show) }
    }

    fun setDisplayMode(mode: Long) {
        setFlags { flags -> flags.setDisplayMode(mode) }
    }

    /** Sorts by [sort], or flips the order when the chapters are sorted by it already. */
    fun setSorting(sort: Long) {
        setFlags { flags -> flags.setSortingModeOrFlipOrder(sort) }
    }

    fun setExcludedScanlators(excludedScanlators: Set<String>) {
        val loaded = state.loaded ?: return
        scope.launchIO {
            entryScanlatorFilterFeature.setExcludedScanlators(
                scope = EntryScanlatorFilterScope(loaded.entry, members.displayedIds()),
                excludedScanlators = excludedScanlators,
            )
        }
    }

    fun setCurrentSettingsAsDefault(applyToExisting: Boolean) {
        val entry = state.loaded?.entry ?: return
        scope.launchNonCancellable {
            libraryPreferences.setChapterSettingsDefault(entry)
            if (applyToExisting) {
                setEntryDefaultChapterFlags.awaitWithLibrary(members.displayedIds())
            }
            snackbarHostState.showSnackbar(
                message = context.stringResource(
                    entry.type.entryTypePresentation(entryTypePresentationFeature).settingsUpdatedLabel,
                ),
            )
        }
    }

    fun resetToDefaultSettings() {
        scope.launchNonCancellable {
            setEntryDefaultChapterFlags.await(members.displayedIds())
        }
    }

    private fun setFlags(transform: (Long) -> Long) {
        state.loaded ?: return
        scope.launchNonCancellable {
            members.forEachDisplayed { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, transform(memberEntry.chapterFlags))
            }
        }
    }
}

private fun Long.setUnreadFilter(flag: Long): Long {
    return this and Entry.CHAPTER_UNREAD_MASK.inv() or flag
}

private fun Long.setDownloadedFilter(flag: Long): Long {
    return this and Entry.CHAPTER_DOWNLOADED_MASK.inv() or flag
}

private fun Long.setBookmarkFilter(flag: Long): Long {
    return this and Entry.CHAPTER_BOOKMARKED_MASK.inv() or flag
}

private fun Long.setFillerFilter(flag: Long): Long {
    return this and Entry.CHAPTER_FILLER_MASK.inv() or flag
}

private fun Long.setShowChapterPreviews(show: Boolean): Long {
    return if (show) this and Entry.CHAPTER_HIDE_PREVIEWS.inv() else this or Entry.CHAPTER_HIDE_PREVIEWS
}

private fun Long.setDisplayMode(mode: Long): Long {
    return this and Entry.CHAPTER_DISPLAY_MASK.inv() or mode
}

private fun Long.setSortingModeOrFlipOrder(sort: Long): Long {
    return if (this and Entry.CHAPTER_SORTING_MASK == sort) {
        this xor Entry.CHAPTER_SORT_DIR_MASK
    } else {
        this and Entry.CHAPTER_SORTING_MASK.inv() or sort
    }
}
