package eu.kanade.tachiyomi

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationFeature
import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.updates.interactor.GetUpdates

/** What the application reads from the graph while it starts. */
interface AppStartupGraph {
    @AppCoroutineScope
    val appScope: CoroutineScope

    val basePreferences: BasePreferences
    val privacyPreferences: PrivacyPreferences
    val networkPreferences: NetworkPreferences
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val preferenceStore: PreferenceStore
    val networkHelper: NetworkHelper
    val extensionManager: ExtensionManager
    val getUpdates: GetUpdates
    val libraryUpdateNotificationFeature: EntryLibraryUpdateNotificationFeature
    val sourceManager: SourceManager
    val database: Database
    val featureRuntimeComposition: FeatureRuntimeComposition
}
