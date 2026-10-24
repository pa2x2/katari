package mihon.entry.interactions.navigation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.runtime.EntryInteractions
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort
import tachiyomi.domain.library.service.LibraryPreferences

@ContributesTo(AppScope::class)
@BindingContainer
object EntryNavigationBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun openFeature(
        openProcessors: Map<EntryType, EntryOpenProcessor>,
        interactions: EntryInteractions,
    ): EntryOpenFeature = DefaultEntryOpenFeature(
        applicableTypes = openProcessors.keys,
        interaction = interactions.open,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun continueFeature(
        continueProcessors: Map<EntryType, EntryContinueProcessor>,
        interactions: EntryInteractions,
        childOwnership: EntryChildOwnershipResolutionPort,
        chapterRepository: EntryChapterRepository,
        progressRepository: EntryProgressRepository,
        libraryPreferences: LibraryPreferences,
        downloadRuntime: EntryDownloadRuntimeFeature,
    ): EntryContinueFeature = DefaultEntryContinueFeature(
        applicableTypes = continueProcessors.keys,
        interaction = interactions.continueEntry,
        batchPreparation = DefaultEntryContinueBatchPreparation(
            childOwnership = childOwnership,
            chapterRepository = chapterRepository,
            progressRepository = progressRepository,
            chapterVisibility = EntryContinueChapterVisibility(libraryPreferences, downloadRuntime),
        ),
    )
}
