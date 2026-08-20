package eu.kanade.tachiyomi.ui.library

import eu.kanade.presentation.library.components.LibraryDisplaySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences

internal data class LibraryFilterPreferences(
    val skipOutsideReleasePeriod: Boolean,
    val globalFilterDownloaded: Boolean,
    val filterDownloaded: TriState,
    val filterUnread: TriState,
    val filterNotStarted: TriState,
    val filterBookmarked: TriState,
    val filterCompleted: TriState,
    val filterIntervalCustom: TriState,
)

internal fun observeLibraryFilterPreferences(
    preferences: LibraryPreferences,
): Flow<LibraryFilterPreferences> {
    return combine(
        preferences.autoUpdateEntryRestrictions.changes(),
        preferences.downloadedOnly.changes(),
        preferences.filterDownloaded.changes(),
        preferences.filterUnread.changes(),
        preferences.filterNotStarted.changes(),
        preferences.filterBookmarked.changes(),
        preferences.filterCompleted.changes(),
        preferences.filterIntervalCustom.changes(),
    ) { values ->
        LibraryFilterPreferences(
            skipOutsideReleasePeriod = LibraryPreferences.ENTRY_OUTSIDE_RELEASE_PERIOD in (values[0] as Set<*>),
            globalFilterDownloaded = values[1] as Boolean,
            filterDownloaded = values[2] as TriState,
            filterUnread = values[3] as TriState,
            filterNotStarted = values[4] as TriState,
            filterBookmarked = values[5] as TriState,
            filterCompleted = values[6] as TriState,
            filterIntervalCustom = values[7] as TriState,
        )
    }.distinctUntilChanged()
}

internal fun observeLibraryDisplaySettings(
    preferences: LibraryPreferences,
): Flow<LibraryDisplaySettings> {
    val badgeSettings = combine(
        preferences.downloadBadge.changes(),
        preferences.unreadBadge.changes(),
        preferences.localBadge.changes(),
        preferences.languageBadge.changes(),
        preferences.entryTypeBadge.changes(),
    ) { downloadBadge, unreadBadge, localBadge, languageBadge, entryTypeBadge ->
        LibraryDisplaySettings(
            downloadBadge = downloadBadge,
            unreadBadge = unreadBadge,
            localBadge = localBadge,
            languageBadge = languageBadge,
            entryTypeBadge = entryTypeBadge,
        )
    }
    return combine(
        badgeSettings,
        preferences.pinnedDisplayStyle.changes(),
    ) { settings, pinnedDisplayStyle ->
        settings.copy(pinnedDisplayStyle = pinnedDisplayStyle)
    }.distinctUntilChanged()
}
