package eu.kanade.tachiyomi.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.components.AppStateBanners
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.home.navigation.HomeSideNavigationHost
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.app.ui.di.LocalAppUiGraph
import mihon.feature.profiles.ui.switcher.ProfileSwitcherHost
import mihon.presentation.kit.layout.ScreenFrame
import mihon.presentation.kit.navigation.Scaffold

/**
 * The screens of [navigator], a window's root navigator around [HomeScreen], as the window shows them once a profile
 * is open: the app state banners above them, the side dock beside them, the profile switcher over them. [overlay]
 * draws over the screens, such as a scrim behind a see-through system navigation bar.
 */
@Composable
fun HomeNavigatorContent(
    navigator: Navigator,
    banners: AppStateBannerState,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val graph = LocalAppUiGraph.current

    LaunchedEffect(navigator.lastItem) {
        (navigator.lastItem as? CatalogScreen)?.sourceId
            .let(graph.getIncognitoState::subscribe)
            .collectLatest { banners.incognito = it }
    }

    // The switcher sits above every screen, so the side dock can open it from any of them
    ProfileSwitcherHost(
        navigator = navigator,
        onManageProfiles = { navigator.push(SettingsScreen(SettingsScreen.Destination.Profiles)) },
    ) {
        val scaffoldInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)
        Scaffold(
            frame = ScreenFrame.Window,
            topBar = {
                AppStateBanners(
                    downloadedOnlyMode = banners.downloadedOnly,
                    incognitoMode = banners.incognito,
                    indexing = banners.indexing,
                    modifier = Modifier.windowInsetsPadding(scaffoldInsets),
                )
            },
            contentWindowInsets = scaffoldInsets,
        ) { contentPadding ->
            // Consume insets already used by app state banners
            Box {
                HomeSideNavigationHost(
                    navigator = navigator,
                    modifier = Modifier
                        .padding(contentPadding)
                        .consumeWindowInsets(contentPadding),
                )

                overlay()
            }
        }
    }

    // Pop source-related screens when incognito mode is turned off
    LaunchedEffect(Unit) {
        graph.incognitoPreferences.incognitoMode.changes()
            .drop(1)
            .filter { !it }
            .onEach {
                val currentScreen = navigator.lastItem
                if (currentScreen is CatalogScreen ||
                    (currentScreen is EntryScreen && currentScreen.fromSource)
                ) {
                    navigator.popUntilRoot()
                }
            }
            .launchIn(this)
    }
}
