package eu.kanade.presentation.more.settings.screen.advanced

import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import mihon.core.common.GlobalCustomPreferences
import mihon.core.image.progressive.ProgressiveImageEngine
import mihon.core.image.progressive.ProgressiveImagePreferences
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.merge.EntryMergeConsequenceStatusFeature
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import tachiyomi.domain.library.service.MetadataUpdateRunner

/** Graph entry points of the advanced settings. */
interface AdvancedSettingsGraph {
    val networkPreferences: NetworkPreferences
    val networkHelper: NetworkHelper
    val globalCustomPreferences: GlobalCustomPreferences
    val entryDownloadMaintenanceFeature: EntryDownloadMaintenanceFeature
    val entryMergeConsequenceStatusFeature: EntryMergeConsequenceStatusFeature
    val metadataUpdateRunner: MetadataUpdateRunner
    val trustExtension: TrustExtension
    val appNotifications: AppNotifications
    val backgroundWorkRestrictions: BackgroundWorkRestrictions
    val webViewData: WebViewData
    val readerBasePreferences: ReaderBasePreferences
    val progressiveImagePreferences: ProgressiveImagePreferences
    val progressiveImageEngine: ProgressiveImageEngine
    val hardwareBitmapSetting: HardwareBitmapSetting
    val extensionInstallerSetting: ExtensionInstallerSetting
    val displayBackendSetting: DisplayBackendSetting
}
