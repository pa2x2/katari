package app.katari.entry.interactions.state

import app.katari.entry.interactions.download.EntryDownloadLifecycleEventSink
import app.katari.entry.interactions.history.EntryHistoryFeature
import app.katari.entry.interactions.runtime.EntryInteractions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
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
        chapters: EntryChapterRepository,
    ): EntryConsumptionFeature = DefaultEntryConsumptionFeature(
        applicableTypes = consumptionProcessors.keys,
        interaction = interactions.consumption,
        downloadLifecycle = downloadLifecycle,
        history = history,
        chapters = chapters,
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
