package eu.kanade.tachiyomi

import app.katari.profiles.sourcepreferences.SourcePreferenceFiles
import app.katari.ui.host.AppProcessGraph
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import tachiyomi.domain.updates.interactor.GetUpdates

/** What the application reads from the graph while it starts, beside what every platform's start reads. */
interface AppStartupGraph : AppProcessGraph {
    val basePreferences: BasePreferences
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val sourcePreferenceFiles: SourcePreferenceFiles
    val extensionManager: ExtensionManager
    val getUpdates: GetUpdates
}
