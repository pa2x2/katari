package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.domain.source.service.ProfileHiddenSourceIds
import eu.kanade.presentation.browse.components.AndroidSourceRows
import eu.kanade.presentation.browse.components.SourceRows
import eu.kanade.tachiyomi.extension.ExtensionNsfwSourceIds
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferenceHosts
import mihon.entry.interactions.source.EntrySourceSettingsFeature
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

        // A source's preferences are built with the activity showing them, as in androidx's own screen.
        @Provides
        fun sourcePreferenceHosts(settingsFeature: EntrySourceSettingsFeature): SourcePreferenceHosts =
            SourcePreferenceHosts(settingsFeature) { context -> context }
    }
}
