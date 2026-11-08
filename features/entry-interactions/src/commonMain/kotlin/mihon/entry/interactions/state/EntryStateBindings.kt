package mihon.entry.interactions.state

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.history.EntryHistoryFeature
import mihon.entry.interactions.runtime.EntryInteractions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences

@ContributesTo(AppScope::class)
@BindingContainer
object EntryStateBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun consumptionFeature(
        consumptionProcessors: Map<EntryType, EntryConsumptionProcessor>,
        interactions: EntryInteractions,
        downloadLifecycle: EntryDownloadLifecycleEventSink,
        history: EntryHistoryFeature,
    ): EntryConsumptionFeature = DefaultEntryConsumptionFeature(
        applicableTypes = consumptionProcessors.keys,
        interaction = interactions.consumption,
        downloadLifecycle = downloadLifecycle,
        history = history,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun bookmarkFeature(
        bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
        interactions: EntryInteractions,
    ): EntryBookmarkFeature =
        DefaultEntryBookmarkFeature(
            applicableTypes = bookmarkProcessors.keys,
            interaction = interactions.bookmark,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun updateEligibilityFeature(): EntryUpdateEligibilityFeature = DefaultEntryUpdateEligibilityFeature()

    @Provides
    @SingleIn(AppScope::class)
    fun progressFeature(
        progressProcessors: Map<EntryType, EntryProgressProcessor>,
        migrationProviders: Map<EntryType, EntryMigrationProvider>,
        interactions: EntryInteractions,
        repository: EntryProgressRepository,
        getEntryWithChapters: GetEntryWithChapters,
        globalLibraryPreferences: GlobalLibraryPreferences,
    ): EntryProgressFeature = DefaultEntryProgressFeature(
        applicableTypes = progressProcessors.keys,
        migrationTypes = progressProcessors.keys intersect migrationProviders.keys,
        interaction = interactions.progress,
        repository = repository,
        getEntryWithChapters = getEntryWithChapters,
        globalLibraryPreferences = globalLibraryPreferences,
    )
}
