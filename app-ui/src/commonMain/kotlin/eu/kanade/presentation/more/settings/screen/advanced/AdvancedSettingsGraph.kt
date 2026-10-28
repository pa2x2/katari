package eu.kanade.presentation.more.settings.screen.advanced

import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.more.OnboardingDestination
import mihon.core.common.GlobalCustomPreferences
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.merge.EntryMergeConsequenceStatusFeature
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
    val onboardingDestination: OnboardingDestination
    val appNotifications: AppNotifications
    val backgroundWorkRestrictions: BackgroundWorkRestrictions
    val webViewData: WebViewData
    val readerDecodingSettings: ReaderDecodingSettings
    val extensionInstallerSetting: ExtensionInstallerSetting
}
