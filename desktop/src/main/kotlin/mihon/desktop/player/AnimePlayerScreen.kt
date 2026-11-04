package mihon.desktop.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.home.navigation.FullWindowScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.interactions.player.AnimePlayer
import mihon.entry.interactions.player.AnimePlayerNavigation
import mihon.entry.interactions.player.AnimePlayerRequest
import mihon.entry.interactions.settings.AnimePlayerPreferences

/**
 * The player filling the window, the dock stepped aside. Its settings and the web view open over it, as Android's
 * open over the player's activity; the screens that fix a source's problem take its place, as Android's leave the
 * player for the main screen.
 */
class AnimePlayerScreen(
    private val request: AnimePlayerRequest,
    private val player: AnimePlayer,
    private val webViewDestination: WebViewDestination,
) : Screen(), FullWindowScreen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val navigation = remember(navigator) { WindowPlayerNavigation(navigator, scope, webViewDestination) }
        player.Content(request, navigation)
    }
}

private class WindowPlayerNavigation(
    private val navigator: Navigator,
    private val scope: CoroutineScope,
    private val webViewDestination: WebViewDestination,
) : AnimePlayerNavigation {
    override val webViewAvailable: Boolean get() = webViewDestination.isAvailable

    override fun close() {
        navigator.pop()
    }

    override fun openPlayerSettings() {
        navigator.push(
            SettingsScreen(
                destination = SettingsScreen.Destination.Readers,
                viewerSettingsSurfaceId = AnimePlayerPreferences.PROVIDER_ID,
                viewerSettingsHighlight = null,
            ),
        )
    }

    override fun openExtensions() {
        navigator.popUntilRoot()
        scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse(BrowseTab.Page.Extensions)) }
    }

    override fun openEntry(entryId: Long) {
        navigator.replace(EntryScreen(entryId))
    }

    override fun openSourceSettings(sourceId: Long) {
        navigator.replace(SourcePreferencesScreen(sourceId))
    }

    override fun openWebView(url: String, title: String?, sourceId: Long?) {
        navigator.push(webViewDestination.screen(url, title, sourceId, headers = emptyMap()))
    }
}
