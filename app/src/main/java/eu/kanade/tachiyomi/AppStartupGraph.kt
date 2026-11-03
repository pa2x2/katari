package eu.kanade.tachiyomi

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import mihon.app.ui.host.AppProcessGraph
import mihon.core.metro.AppCoroutineScope
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import tachiyomi.domain.updates.interactor.GetUpdates

/** What the application reads from the graph while it starts, beside what every platform's start reads. */
interface AppStartupGraph : AppProcessGraph {
    @AppCoroutineScope
    val appScope: CoroutineScope

    val basePreferences: BasePreferences
    val privacyPreferences: PrivacyPreferences
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val sourcePreferenceFiles: SourcePreferenceFiles
    val extensionManager: ExtensionManager
    val getUpdates: GetUpdates
}
