package eu.kanade.domain

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
import eu.kanade.domain.source.service.ProfileHiddenSourceIds
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.SyncChapterProgressWithTrack
import eu.kanade.domain.track.interactor.TrackChapter
import mihon.data.extension.repository.ExtensionStoreRepositoryImpl
import mihon.data.extension.service.ExtensionStoreService
import mihon.domain.extension.interactor.AddExtensionStore
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.domain.extension.interactor.GetExtensionStores
import mihon.domain.extension.interactor.RemoveExtensionStore
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.domain.upcoming.interactor.GetUpcomingEntries
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import tachiyomi.data.category.CategoryRepositoryImpl
import tachiyomi.data.entry.DownloadPreferencesRepositoryImpl
import tachiyomi.data.entry.EntryChapterRepositoryImpl
import tachiyomi.data.entry.EntryCoverHashesRepositoryImpl
import tachiyomi.data.entry.EntryProgressRepositoryImpl
import tachiyomi.data.entry.EntryRepositoryImpl
import tachiyomi.data.entry.EntrySyncRepositoryImpl
import tachiyomi.data.entry.PlaybackPreferencesRepositoryImpl
import tachiyomi.data.entry.ViewerSettingOverrideRepositoryImpl
import tachiyomi.data.history.HistoryRepositoryImpl
import tachiyomi.data.release.ReleaseServiceImpl
import tachiyomi.data.source.SourceRepositoryImpl
import tachiyomi.data.source.StubSourceRepositoryImpl
import tachiyomi.data.track.TrackRepositoryImpl
import tachiyomi.data.updates.UpdatesRepositoryImpl
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.interactor.UpdateCategory
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
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryCoverHashesRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntrySyncRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.GetTotalReadDuration
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.service.ReleaseService
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryEntries
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerEntry
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.repository.TrackRepository
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.repository.UpdatesRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<CategoryRepository> { CategoryRepositoryImpl(get(), get()) }
        addFactory { GetCategories(get()) }
        addFactory { ResetCategoryFlags(get(), get()) }
        addFactory { SetDisplayMode(get()) }
        addFactory { SetSortModeForCategory(get(), get()) }
        addFactory { CreateCategoryWithName(get(), get()) }
        addFactory { RenameCategory(get()) }
        addFactory { ReorderCategory(get()) }
        addFactory { UpdateCategory(get()) }
        addFactory { DeleteCategory(get(), get(), get()) }

        addSingletonFactory<EntryRepository> { EntryRepositoryImpl(get(), get()) }
        addSingletonFactory<EntryChapterRepository> { EntryChapterRepositoryImpl(get(), get()) }
        addSingletonFactory<EntryProgressRepository> { EntryProgressRepositoryImpl(get()) }
        addSingletonFactory<PlaybackPreferencesRepository> { PlaybackPreferencesRepositoryImpl(get()) }
        addSingletonFactory<ViewerSettingOverrideRepository> { ViewerSettingOverrideRepositoryImpl(get()) }
        addSingletonFactory<DownloadPreferencesRepository> { DownloadPreferencesRepositoryImpl(get()) }
        addSingletonFactory<EntrySyncRepository> { EntrySyncRepositoryImpl(get(), get()) }
        addSingletonFactory<EntryCoverHashesRepository> { EntryCoverHashesRepositoryImpl(get(), get()) }

        addFactory { GetEntry(get()) }
        addFactory { GetEntryById(get()) }
        addFactory { GetNextUnreadChapter(get()) }
        addFactory { SetEntryCategories(get()) }
        addFactory { SetEntryChapterFlags(get()) }
        addFactory { NetworkToLocalEntry(get()) }
        addFactory { GetLibraryEntries(get(), get(), get(), get(), get(), get(), get(), get()) }
        addFactory { SyncEntryWithSource(get(), get(), get(), get(), get(), get(), get()) }

        addFactory { GetEntryWithChapters(get(), get()) }

        addFactory { GetNextChapters(get(), get(), get(), get()) }
        addFactory { GetUpcomingEntries(get()) }
        addFactory { FetchInterval(get()) }
        addFactory { SetEntryViewerFlags(get()) }
        addFactory { UpdateEntry(get(), get()) }
        addSingletonFactory<ReleaseService> { ReleaseServiceImpl(get(), get()) }
        addFactory { GetApplicationRelease(get(), get()) }

        addSingletonFactory<TrackRepository> { TrackRepositoryImpl(get(), get()) }
        addFactory { TrackChapter(get(), get(), get(), get()) }
        addFactory { AddTracks(get(), get()) }
        addFactory { RefreshTracks(get(), get(), get()) }
        addFactory { DeleteTrack(get()) }
        addFactory { GetTracksPerEntry(get()) }
        addFactory { GetTracks(get()) }
        addFactory { InsertTrack(get()) }
        addFactory { SyncChapterProgressWithTrack(get(), get(), get(), get()) }

        addSingletonFactory<HistoryRepository> { HistoryRepositoryImpl(get(), get()) }
        addFactory { GetHistory(get(), get()) }
        addFactory { UpsertHistory(get()) }
        addFactory { RemoveHistory(get()) }
        addFactory { GetTotalReadDuration(get()) }

        addFactory { GetExtensionsByType(get(), get()) }
        addFactory { GetExtensionSources(get()) }
        addFactory { GetExtensionLanguages(get(), get()) }

        addSingletonFactory<UpdatesRepository> { UpdatesRepositoryImpl(get(), get()) }
        addFactory { GetUpdates(get(), get()) }

        addSingletonFactory<HiddenSourceIds> { ProfileHiddenSourceIds(get()) }
        addSingletonFactory<SourceRepository> { SourceRepositoryImpl(get(), get(), get(), get()) }
        addSingletonFactory<StubSourceRepository> { StubSourceRepositoryImpl(get()) }
        addFactory { GetEnabledSources(get(), get()) }
        addFactory { GetEnabledCatalogSources(get(), get(), get()) }
        addSingletonFactory { BrowseFeedService(get(), get()) }
        addFactory { GetLanguagesWithSources(get(), get()) }
        addFactory { GetLanguagesWithCatalogSources(get(), get()) }
        addFactory { GetSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetSourcesWithNonLibraryEntries(get(), get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { ToggleLanguage(get()) }
        addFactory { ToggleSource(get()) }
        addFactory { ToggleSourcePin(get()) }
        addFactory { TrustExtension(get(), get()) }

        addSingletonFactory { ExtensionStoreService(get(), get(), get()) }
        addSingletonFactory<ExtensionStoreRepository> { ExtensionStoreRepositoryImpl(get(), get()) }
        addFactory { AddExtensionStore(get()) }
        addFactory { GetExtensionStoreCountAsFlow(get()) }
        addFactory { GetExtensionStores(get()) }
        addFactory { RemoveExtensionStore(get()) }
        addFactory { UpdateExtensionStores(get()) }

        addFactory { ToggleIncognito(get()) }
        addFactory { GetIncognitoState(get(), get(), get()) }
    }
}
