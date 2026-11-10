package eu.kanade.presentation.more.settings.screen.advanced

import app.katari.core.common.GlobalCustomPreferences
import app.katari.core.image.progressive.ProgressiveImageEngine
import app.katari.core.image.progressive.ProgressiveImagePreferences
import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.merge.EntryMergeConsequenceStatusFeature
import app.katari.entry.interactions.reader.settings.ReaderBasePreferences
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
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
    val backgroundWorkRestrictions: BackgroundWorkRestrictions
    val webViewData: WebViewData
    val readerBasePreferences: ReaderBasePreferences
    val progressiveImagePreferences: ProgressiveImagePreferences
    val progressiveImageEngine: ProgressiveImageEngine
    val hardwareBitmapSetting: HardwareBitmapSetting
    val extensionInstallerSetting: ExtensionInstallerSetting
}
