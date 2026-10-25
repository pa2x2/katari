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
import mihon.data.extension.repository.ExtensionStoreRepositoryImpl
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import tachiyomi.data.category.CategoryRepositoryImpl
import tachiyomi.data.entry.ChapterGroupExpansionRepositoryImpl
import tachiyomi.data.entry.DownloadPreferencesRepositoryImpl
import tachiyomi.data.entry.EntryChapterGroupRepositoryImpl
import tachiyomi.data.entry.EntryChapterListWalkRepositoryImpl
import tachiyomi.data.entry.EntryChapterRepositoryImpl
import tachiyomi.data.entry.EntryCoverHashesRepositoryImpl
import tachiyomi.data.entry.EntryProgressRepositoryImpl
import tachiyomi.data.entry.EntryRepositoryImpl
import tachiyomi.data.entry.EntrySyncRepositoryImpl
import tachiyomi.data.entry.EntryTranslationLanguagesRepositoryImpl
import tachiyomi.data.entry.EntryTranslationQueueRepositoryImpl
import tachiyomi.data.entry.MergedMemberExpansionRepositoryImpl
import tachiyomi.data.entry.PlaybackPreferencesRepositoryImpl
import tachiyomi.data.entry.ViewerSettingOverrideRepositoryImpl
import tachiyomi.data.history.HistoryRepositoryImpl
import tachiyomi.data.history.activity.HistoryActivityBackupRepositoryImpl
import tachiyomi.data.history.activity.HistoryActivityRepositoryImpl
import tachiyomi.data.library.update.LibraryUpdateReportRepositoryImpl
import tachiyomi.data.library.update.LibraryUpdateRulesRepositoryImpl
import tachiyomi.data.source.SourceRepositoryImpl
import tachiyomi.data.source.StubSourceRepositoryImpl
import tachiyomi.data.source.identity.SourceIdentityRepositoryImpl
import tachiyomi.data.statistics.StatisticsRepositoryImpl
import tachiyomi.data.statistics.entry.EntryActivityRepositoryImpl
import tachiyomi.data.statistics.recap.StatisticsRecapRepositoryImpl
import tachiyomi.data.track.TrackRepositoryImpl
import tachiyomi.data.updates.UpdatesRepositoryImpl
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.repository.ChapterGroupExpansionRepository
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryCoverHashesRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntrySyncRepository
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository
import tachiyomi.domain.history.repository.HistoryActivityRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.statistics.entry.EntryActivityRepository
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import tachiyomi.domain.statistics.repository.StatisticsRepository
import tachiyomi.domain.track.repository.TrackRepository
import tachiyomi.domain.updates.repository.UpdatesRepository

/**
 * Production repository implementations. Validation graphs supply controlled ones instead, so these are not
 * contributed.
 */
@BindingContainer
interface DataBindings {
    @Binds
    val CategoryRepositoryImpl.categoryRepository: CategoryRepository

    @Binds
    val LibraryUpdateRulesRepositoryImpl.libraryUpdateRulesRepository: LibraryUpdateRulesRepository

    @Binds
    val LibraryUpdateReportRepositoryImpl.libraryUpdateReportRepository: LibraryUpdateReportRepository

    @Binds
    val EntryRepositoryImpl.entryRepository: EntryRepository

    @Binds
    val EntryChapterRepositoryImpl.entryChapterRepository: EntryChapterRepository

    @Binds
    val EntryProgressRepositoryImpl.entryProgressRepository: EntryProgressRepository

    @Binds
    val PlaybackPreferencesRepositoryImpl.playbackPreferencesRepository: PlaybackPreferencesRepository

    @Binds
    val EntryTranslationLanguagesRepositoryImpl.entryTranslationLanguagesRepository: EntryTranslationLanguagesRepository

    @Binds
    val EntryTranslationQueueRepositoryImpl.entryTranslationQueueRepository: EntryTranslationQueueRepository

    @Binds
    val ViewerSettingOverrideRepositoryImpl.viewerSettingOverrideRepository: ViewerSettingOverrideRepository

    @Binds
    val DownloadPreferencesRepositoryImpl.downloadPreferencesRepository: DownloadPreferencesRepository

    @Binds
    val EntrySyncRepositoryImpl.entrySyncRepository: EntrySyncRepository

    @Binds
    val EntryCoverHashesRepositoryImpl.entryCoverHashesRepository: EntryCoverHashesRepository

    @Binds
    val MergedMemberExpansionRepositoryImpl.mergedMemberExpansionRepository: MergedMemberExpansionRepository

    @Binds
    val EntryChapterGroupRepositoryImpl.entryChapterGroupRepository: EntryChapterGroupRepository

    @Binds
    val EntryChapterListWalkRepositoryImpl.entryChapterListWalkRepository: EntryChapterListWalkRepository

    @Binds
    val ChapterGroupExpansionRepositoryImpl.chapterGroupExpansionRepository: ChapterGroupExpansionRepository

    @Binds
    val TrackRepositoryImpl.trackRepository: TrackRepository

    @Binds
    val HistoryRepositoryImpl.historyRepository: HistoryRepository

    @Binds
    val HistoryActivityBackupRepositoryImpl.historyActivityBackupRepository: HistoryActivityBackupRepository

    @Binds
    val HistoryActivityRepositoryImpl.historyActivityRepository: HistoryActivityRepository

    @Binds
    val StatisticsRepositoryImpl.statisticsRepository: StatisticsRepository

    @Binds
    val StatisticsRecapRepositoryImpl.statisticsRecapRepository: StatisticsRecapRepository

    @Binds
    val EntryActivityRepositoryImpl.entryActivityRepository: EntryActivityRepository

    @Binds
    val UpdatesRepositoryImpl.updatesRepository: UpdatesRepository

    @Binds
    val ProfileHiddenSourceIds.hiddenSourceIds: HiddenSourceIds

    @Binds
    val SourceRepositoryImpl.sourceRepository: SourceRepository

    @Binds
    val StubSourceRepositoryImpl.stubSourceRepository: StubSourceRepository

    @Binds
    val SourceIdentityRepositoryImpl.sourceIdentityRepository: SourceIdentityRepository

    @Binds
    val ExtensionStoreRepositoryImpl.extensionStoreRepository: ExtensionStoreRepository

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
        )
    }
}
