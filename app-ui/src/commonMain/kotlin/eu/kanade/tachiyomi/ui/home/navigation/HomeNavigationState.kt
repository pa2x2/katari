package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import mihon.core.common.HomeScreenTabs
import mihon.core.common.navigation.HomeNavigationConfiguration

/**
 * Connects the home destinations shown beside every screen of a wide window to the home tabs, which aren't composed
 * while another screen is on top. Home publishes what it shows here, and a tap on the side dock waits in
 * [requestedTab] until home is back on top to carry it out.
 */
@Stable
class HomeNavigationState {
    /** Null until home is first shown. */
    var configuration by mutableStateOf<HomeNavigationConfiguration?>(null)
    var selectedTab by mutableStateOf<HomeScreenTabs?>(null)

    /** Set by a home tab that takes the window for itself, such as while items are selected. */
    var hiddenByTab by mutableStateOf(false)

    var requestedTab by mutableStateOf<HomeScreenTabs?>(null)
}

val LocalHomeNavigation = staticCompositionLocalOf<HomeNavigationState> {
    error("HomeNavigationState is provided by HomeSideNavigationHost")
}
