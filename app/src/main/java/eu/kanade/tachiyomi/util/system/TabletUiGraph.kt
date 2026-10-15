package eu.kanade.tachiyomi.util.system

import eu.kanade.domain.ui.UiPreferences

/** Graph entry point of the tablet UI override, which activities apply before they can be injected. */
interface TabletUiGraph {
    val uiPreferences: UiPreferences
}
