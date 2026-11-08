package app.katari.entry.interactions.navigation

import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.entry.interactions.runtime.EntryInteractions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
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
        viewerStandIns: Map<EntryType, EntryViewerStandIn>,
        interactions: EntryInteractions,
    ): EntryOpenFeature = DefaultEntryOpenFeature(
        viewerTypes = openProcessors.keys,
        standInTypes = viewerStandIns.keys,
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
        openFeature: EntryOpenFeature,
    ): EntryContinueFeature = DefaultEntryContinueFeature(
        applicableTypes = continueProcessors.keys,
        interaction = interactions.continueEntry,
        batchPreparation = DefaultEntryContinueBatchPreparation(
            childOwnership = childOwnership,
            chapterRepository = chapterRepository,
            progressRepository = progressRepository,
            chapterVisibility = EntryContinueChapterVisibility(libraryPreferences, downloadRuntime),
        ),
        openFeature = openFeature,
    )
}

@ContributesTo(AppScope::class)
@BindingContainer
interface EntryViewerStandInBindings {
    // A platform with every type's viewer has none.
    @Multibinds(allowEmpty = true)
    fun viewerStandIns(): Map<EntryType, EntryViewerStandIn>
}
