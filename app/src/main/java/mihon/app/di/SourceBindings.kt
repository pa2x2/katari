package mihon.app.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.interactor.GetEnabledCatalogSources
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.ProfileHiddenSourceIds
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.components.AndroidSourceRows
import eu.kanade.presentation.browse.components.SourceRows
import eu.kanade.tachiyomi.extension.ExtensionNsfwSourceIds
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.ChapterListWalkLocks
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.NsfwSourceIds
import tachiyomi.domain.source.service.SourceManager

/** Source services that need the app's own code or an explicit constructor call. */
@BindingContainer
interface SourceBindings {
    @Binds
    val ProfileHiddenSourceIds.hiddenSourceIds: HiddenSourceIds

    @Binds
    val ExtensionNsfwSourceIds.nsfwSourceIds: NsfwSourceIds

    @Binds
    val AndroidSourceRows.sourceRows: SourceRows

    companion object {
        // Both take the per-profile source preferences as an optional parameter for tests; production passes them.
        @Provides
        fun getEnabledCatalogSources(
            repository: SourceRepository,
            preferences: SourcePreferences,
            profilePreferences: ProfileSourcePreferences,
        ): GetEnabledCatalogSources = GetEnabledCatalogSources(repository, preferences, profilePreferences)

        @Provides
        @SingleIn(AppScope::class)
        fun browseFeedService(
            preferences: SourcePreferences,
            profilePreferences: ProfileSourcePreferences,
        ): BrowseFeedService = BrowseFeedService(preferences, profilePreferences)

        // Constructed explicitly so the clock keeps its default instead of resolving `() -> Long` from the graph.
        @Provides
        fun syncEntryWithSource(
            entryRepository: EntryRepository,
            entryChapterRepository: EntryChapterRepository,
            entryChapterGroupRepository: EntryChapterGroupRepository,
            entryProgressRepository: EntryProgressRepository,
            sourceManager: SourceManager,
            libraryPreferences: LibraryPreferences,
            fetchInterval: FetchInterval,
            metadataChangeNotifier: EntryMetadataChangeNotifier,
            entryChapterListWalkRepository: EntryChapterListWalkRepository,
            chapterListWalkScheduler: ChapterListWalkScheduler,
            walkLocks: ChapterListWalkLocks,
        ): SyncEntryWithSource = SyncEntryWithSource(
            entryRepository = entryRepository,
            entryChapterRepository = entryChapterRepository,
            entryChapterGroupRepository = entryChapterGroupRepository,
            entryProgressRepository = entryProgressRepository,
            sourceManager = sourceManager,
            libraryPreferences = libraryPreferences,
            fetchInterval = fetchInterval,
            metadataChangeNotifier = metadataChangeNotifier,
            entryChapterListWalkRepository = entryChapterListWalkRepository,
            chapterListWalkScheduler = chapterListWalkScheduler,
            walkLocks = walkLocks,
        )
    }
}
