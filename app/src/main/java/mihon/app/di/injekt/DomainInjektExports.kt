package mihon.app.di.injekt

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.interactor.GetEnabledCatalogSources
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.source.interactor.GetLanguagesWithCatalogSources
import eu.kanade.domain.source.interactor.GetLanguagesWithSources
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.SyncChapterProgressWithTrack
import eu.kanade.domain.track.interactor.TrackChapter
import mihon.data.extension.service.ExtensionStoreService
import mihon.domain.extension.interactor.AddExtensionStore
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.domain.extension.interactor.GetExtensionStores
import mihon.domain.extension.interactor.RemoveExtensionStore
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.domain.upcoming.interactor.GetUpcomingEntries
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryById
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.interactor.GetNextUnreadChapter
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.interactor.SetEntryChapterFlags
import tachiyomi.domain.entry.interactor.SetEntryViewerFlags
import tachiyomi.domain.entry.interactor.SetLibraryPinned
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryCoverHashesRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntrySyncRepository
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.GetTotalReadDuration
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository
import tachiyomi.domain.history.repository.HistoryActivityRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryEntries
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.statistics.entry.EntryActivityRepository
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import tachiyomi.domain.statistics.repository.StatisticsRepository
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerEntry
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.repository.TrackRepository
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.repository.UpdatesRepository

/** Serves the domain layer to code that still resolves it through Injekt. Goes away with Injekt. */
@BindingContainer
object DomainInjektExports {
    @Provides
    @IntoSet
    fun categoryRepository(x: () -> CategoryRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getCategories(x: () -> GetCategories): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun resetCategoryFlags(x: () -> ResetCategoryFlags): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun setDisplayMode(x: () -> SetDisplayMode): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun setSortModeForCategory(x: () -> SetSortModeForCategory): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun createCategoryWithName(x: () -> CreateCategoryWithName): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun renameCategory(x: () -> RenameCategory): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun reorderCategory(x: () -> ReorderCategory): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun deleteCategory(x: () -> DeleteCategory): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun libraryUpdateRulesRepository(x: () -> LibraryUpdateRulesRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun libraryUpdateReportRepository(x: () -> LibraryUpdateReportRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryRepository(x: () -> EntryRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryChapterRepository(x: () -> EntryChapterRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryProgressRepository(x: () -> EntryProgressRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun playbackPreferencesRepository(x: () -> PlaybackPreferencesRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryTranslationLanguagesRepository(
        x: () -> EntryTranslationLanguagesRepository,
    ): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryTranslationQueueRepository(x: () -> EntryTranslationQueueRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun viewerSettingOverrideRepository(x: () -> ViewerSettingOverrideRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun downloadPreferencesRepository(x: () -> DownloadPreferencesRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entrySyncRepository(x: () -> EntrySyncRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryCoverHashesRepository(x: () -> EntryCoverHashesRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun mergedMemberExpansionRepository(x: () -> MergedMemberExpansionRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getEntry(x: () -> GetEntry): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getEntryById(x: () -> GetEntryById): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getNextUnreadChapter(x: () -> GetNextUnreadChapter): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun setEntryCategories(x: () -> SetEntryCategories): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun setEntryChapterFlags(x: () -> SetEntryChapterFlags): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun networkToLocalEntry(x: () -> NetworkToLocalEntry): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getLibraryEntries(x: () -> GetLibraryEntries): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun setLibraryPinned(x: () -> SetLibraryPinned): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun syncEntryWithSource(x: () -> SyncEntryWithSource): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getEntryWithChapters(x: () -> GetEntryWithChapters): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getNextChapters(x: () -> GetNextChapters): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getUpcomingEntries(x: () -> GetUpcomingEntries): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun fetchInterval(x: () -> FetchInterval): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun setEntryViewerFlags(x: () -> SetEntryViewerFlags): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun updateEntry(x: () -> UpdateEntry): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun trackRepository(x: () -> TrackRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun trackChapter(x: () -> TrackChapter): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun addTracks(x: () -> AddTracks): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun refreshTracks(x: () -> RefreshTracks): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun deleteTrack(x: () -> DeleteTrack): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getTracksPerEntry(x: () -> GetTracksPerEntry): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getTracks(x: () -> GetTracks): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun insertTrack(x: () -> InsertTrack): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun syncChapterProgressWithTrack(x: () -> SyncChapterProgressWithTrack): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun historyRepository(x: () -> HistoryRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun historyActivityBackupRepository(x: () -> HistoryActivityBackupRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun historyActivityRepository(x: () -> HistoryActivityRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun statisticsRepository(x: () -> StatisticsRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun statisticsRecapRepository(x: () -> StatisticsRecapRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryActivityRepository(x: () -> EntryActivityRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getHistory(x: () -> GetHistory): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun upsertHistory(x: () -> UpsertHistory): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun removeHistory(x: () -> RemoveHistory): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getTotalReadDuration(x: () -> GetTotalReadDuration): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getExtensionsByType(x: () -> GetExtensionsByType): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getExtensionSources(x: () -> GetExtensionSources): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getExtensionLanguages(x: () -> GetExtensionLanguages): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun updatesRepository(x: () -> UpdatesRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getUpdates(x: () -> GetUpdates): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun hiddenSourceIds(x: () -> HiddenSourceIds): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceRepository(x: () -> SourceRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun stubSourceRepository(x: () -> StubSourceRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getEnabledSources(x: () -> GetEnabledSources): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getLanguagesWithSources(x: () -> GetLanguagesWithSources): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getLanguagesWithCatalogSources(x: () -> GetLanguagesWithCatalogSources): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getSourcesWithFavoriteCount(x: () -> GetSourcesWithFavoriteCount): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getSourcesWithNonLibraryEntries(x: () -> GetSourcesWithNonLibraryEntries): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun setMigrateSorting(x: () -> SetMigrateSorting): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun toggleLanguage(x: () -> ToggleLanguage): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun toggleSource(x: () -> ToggleSource): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun toggleSourcePin(x: () -> ToggleSourcePin): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun trustExtension(x: () -> TrustExtension): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun extensionStoreService(x: () -> ExtensionStoreService): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun extensionStoreRepository(x: () -> ExtensionStoreRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun addExtensionStore(x: () -> AddExtensionStore): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getExtensionStoreCountAsFlow(x: () -> GetExtensionStoreCountAsFlow): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getExtensionStores(x: () -> GetExtensionStores): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun removeExtensionStore(x: () -> RemoveExtensionStore): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun updateExtensionStores(x: () -> UpdateExtensionStores): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun toggleIncognito(x: () -> ToggleIncognito): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getIncognitoState(x: () -> GetIncognitoState): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun getEnabledCatalogSources(x: () -> GetEnabledCatalogSources): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun browseFeedService(x: () -> BrowseFeedService): InjektExport<*> = injektExport(x)
}
