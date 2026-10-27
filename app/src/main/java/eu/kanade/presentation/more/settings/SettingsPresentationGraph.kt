package eu.kanade.presentation.more.settings

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import mihon.core.common.CustomPreferences
import mihon.core.common.GlobalCustomPreferences
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.common.image.progressive.ProgressiveImagePreferences
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadSettingsFeature
import mihon.entry.interactions.media.EntryImmersiveFeature
import mihon.entry.interactions.media.EntryMediaCacheFeature
import mihon.entry.interactions.media.EntryPreviewFeature
import mihon.entry.interactions.media.EntryViewerSettingsFeature
import mihon.entry.interactions.merge.EntryMergeConsequenceStatusFeature
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.settings.AnimePlayerDevicePreferences
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import mihon.feature.profiles.core.ProfilesPreferences
import mihon.translation.api.host.TranslationHostActions
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.install.MpvAddonInstallation
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StoragePreferences

/** Graph entry points of the settings screens. */
interface SettingsPresentationGraph {
    val activeProfileProvider: ActiveProfileProvider
    val animePlayerPreferences: AnimePlayerPreferences
    val animePlayerDevicePreferences: AnimePlayerDevicePreferences
    val mpvAddon: MpvAddon
    val mpvAddonInstallation: MpvAddonInstallation
    val backupPreferences: BackupPreferences
    val basePreferences: BasePreferences
    val bookDocumentReaderSettings: BookDocumentReaderSettings
    val bookDocumentReaderFontLibrary: BookDocumentReaderFontLibrary
    val customPreferences: CustomPreferences
    val downloadPreferences: DownloadPreferences
    val duplicatePreferences: DuplicatePreferences
    val entryDownloadMaintenanceFeature: EntryDownloadMaintenanceFeature
    val entryDownloadSettingsFeature: EntryDownloadSettingsFeature
    val entryImmersiveFeature: EntryImmersiveFeature
    val entryMediaCacheFeature: EntryMediaCacheFeature
    val entryMergeConsequenceStatusFeature: EntryMergeConsequenceStatusFeature
    val entryPreviewFeature: EntryPreviewFeature
    val entryRepository: EntryRepository
    val entryTrackingFeature: EntryTrackingFeature
    val entryTranslateFeature: EntryTranslateFeature
    val entryTranslatePreferences: EntryTranslatePreferences
    val entryViewerSettingsFeature: EntryViewerSettingsFeature
    val getCategories: GetCategories
    val getExtensionStoreCountAsFlow: GetExtensionStoreCountAsFlow
    val globalCustomPreferences: GlobalCustomPreferences
    val globalLibraryPreferences: GlobalLibraryPreferences
    val libraryPreferences: LibraryPreferences
    val mangaReaderSettings: MangaReaderSettings
    val networkHelper: NetworkHelper
    val networkPreferences: NetworkPreferences
    val privacyPreferences: PrivacyPreferences
    val profilesPreferences: ProfilesPreferences
    val progressiveImageEngine: ProgressiveImageEngine
    val progressiveImagePreferences: ProgressiveImagePreferences
    val readerSharedSettingsRegistry: ReaderSharedSettingsRegistry
    val resetCategoryFlags: ResetCategoryFlags
    val securityPreferences: SecurityPreferences
    val sourceManager: SourceManager
    val sourcePreferences: SourcePreferences
    val sourceRepository: SourceRepository
    val storagePreferences: StoragePreferences
    val trackPreferences: TrackPreferences
    val translationHostActions: TranslationHostActions
    val trustExtension: TrustExtension
    val uiPreferences: UiPreferences
    val viewerSettingBinder: ViewerSettingBinder
}
