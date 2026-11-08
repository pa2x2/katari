package app.katari.entry.interactions.source

import app.katari.entry.interactions.lifecycle.metadata.EntryMetadataLifecycleFeature
import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
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

/**
 * The refresh mechanics [EntrySourceRefreshBindings] builds the refresh feature on: syncing an entry with its source,
 * telling the metadata lifecycle what changed, and whether a profile's library takes the titles sources report.
 */
@ContributesTo(AppScope::class)
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
    @SingleIn(AppScope::class)
    fun metadataChangeNotifier(metadataLifecycle: EntryMetadataLifecycleFeature): EntryMetadataChangeNotifier =
        EntryMetadataChangeNotifier(metadataLifecycle::changed)

    @Provides
    fun libraryTitleUpdatePolicy(profileStore: ProfileStore): EntryLibraryTitleUpdatePolicy =
        EntryLibraryTitleUpdatePolicy { profileId ->
            LibraryPreferences(profileStore.profileStore(profileId)).updateMangaTitles.get()
        }
}
