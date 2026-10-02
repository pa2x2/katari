package tachiyomi.domain.updates.service

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.preference.getEnumSet
import tachiyomi.core.common.preference.getLongArray
import tachiyomi.domain.updates.model.UpdatesFeedFilter

class UpdatesPreferences(
    preferenceStore: PreferenceStore,
) {

    val filterDownloaded: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_downloaded",
        TriState.DISABLED,
    )

    val filterUnread: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_unread",
        TriState.DISABLED,
    )

    val filterStarted: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_started",
        TriState.DISABLED,
    )

    val filterBookmarked: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_bookmarked",
        TriState.DISABLED,
    )

    val filterIncludedCategories: Preference<List<Long>> = preferenceStore.getLongArray(
        "pref_filter_updates_included_categories",
        emptyList(),
    )

    val filterExcludedCategories: Preference<List<Long>> = preferenceStore.getLongArray(
        "pref_filter_updates_excluded_categories",
        emptyList(),
    )

    val filterIncludedEntryTypes: Preference<Set<EntryType>> = preferenceStore.getEnumSet(
        "pref_filter_updates_included_entry_types",
        emptySet(),
    )

    val filterExcludedEntryTypes: Preference<Set<EntryType>> = preferenceStore.getEnumSet(
        "pref_filter_updates_excluded_entry_types",
        emptySet(),
    )

    val filterIncludedSources: Preference<List<Long>> = preferenceStore.getLongArray(
        "pref_filter_updates_included_sources",
        emptyList(),
    )

    val filterExcludedSources: Preference<List<Long>> = preferenceStore.getLongArray(
        "pref_filter_updates_excluded_sources",
        emptyList(),
    )

    private val feedFilterPreferences: List<Preference<*>>
        get() = listOf(
            filterDownloaded,
            filterUnread,
            filterStarted,
            filterBookmarked,
            filterIncludedCategories,
            filterExcludedCategories,
            filterIncludedEntryTypes,
            filterExcludedEntryTypes,
            filterIncludedSources,
            filterExcludedSources,
        )

    fun feedFilter(): UpdatesFeedFilter = UpdatesFeedFilter(
        downloaded = filterDownloaded.get(),
        unseen = filterUnread.get(),
        started = filterStarted.get(),
        bookmarked = filterBookmarked.get(),
        categories = UpdatesFeedFilter.Selection(
            filterIncludedCategories.get().toSet(),
            filterExcludedCategories.get().toSet(),
        ),
        types = UpdatesFeedFilter.Selection(filterIncludedEntryTypes.get(), filterExcludedEntryTypes.get()),
        sources = UpdatesFeedFilter.Selection(
            filterIncludedSources.get().toSet(),
            filterExcludedSources.get().toSet(),
        ),
    )

    fun feedFilterChanges(): Flow<UpdatesFeedFilter> {
        return merge(*feedFilterPreferences.map { it.changes() }.toTypedArray())
            .map { feedFilter() }
            .distinctUntilChanged()
    }

    fun clearFeedFilter() {
        feedFilterPreferences.forEach { it.delete() }
    }
}
