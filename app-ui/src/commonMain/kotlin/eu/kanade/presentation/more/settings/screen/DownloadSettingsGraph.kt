package eu.kanade.presentation.more.settings.screen

import mihon.core.platform.network.NetworkConnectivity
import mihon.entry.interactions.download.EntryDownloadSettingsFeature
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslatePreferences
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences

/** Graph entry points of the download settings. */
interface DownloadSettingsGraph {
    val getCategories: GetCategories
    val downloadPreferences: DownloadPreferences
    val entryDownloadSettingsFeature: EntryDownloadSettingsFeature
    val entryTranslatePreferences: EntryTranslatePreferences
    val entryTranslateFeature: EntryTranslateFeature
    val networkConnectivity: NetworkConnectivity
}
