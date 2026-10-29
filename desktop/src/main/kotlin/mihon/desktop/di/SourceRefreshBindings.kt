package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.lifecycle.metadata.EntryMetadataLifecycleFeature
import mihon.entry.interactions.source.EntryLibraryTitleUpdatePolicy
import mihon.feature.profiles.core.ProfileStore
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
import tachiyomi.domain.source.service.SourceManager

/** What refreshing entries from their sources asks of the app. */
@BindingContainer
object SourceRefreshBindings {
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

    @Provides
    fun metadataChangeNotifier(metadataLifecycle: EntryMetadataLifecycleFeature): EntryMetadataChangeNotifier =
        EntryMetadataChangeNotifier(metadataLifecycle::changed)

    @Provides
    fun libraryTitleUpdatePolicy(profileStore: ProfileStore): EntryLibraryTitleUpdatePolicy =
        EntryLibraryTitleUpdatePolicy { profileId ->
            LibraryPreferences(profileStore.profileStore(profileId)).updateMangaTitles.get()
        }
}
