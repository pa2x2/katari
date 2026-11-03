package eu.kanade.tachiyomi.ui.home.navigation

import mihon.app.ui.di.AppUiGraph
import mihon.core.common.HomeScreenTabs

/**
 * The home tabs whose feature this platform doesn't offer: the navigation leaves them out, whatever the profile's tab
 * settings say, and its editor doesn't offer to add them.
 */
internal fun AppUiGraph.unofferedHomeTabs(): Set<HomeScreenTabs> = buildSet {
    // A platform that can't translate knows no translation engine, as the translation settings tell it too
    if (translationHostActions.knownEngines.isEmpty()) add(HomeScreenTabs.Translator)
}
