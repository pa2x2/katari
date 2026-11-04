package mihon.desktop.player

import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.interactions.player.AnimePlayerNavigation
import mihon.entry.interactions.settings.AnimePlayerPreferences

/**
 * Where [visit]'s player leaves for: its settings and the web view open over it, as Android's open over the player's
 * activity; the screens that fix a source's problem take its place, as Android's leave the player for the main screen;
 * and picture-in-picture goes through [player].
 */
internal class WindowPlayerNavigation(
    private val player: WindowAnimePlayer,
    private val visit: AnimePlayerVisit,
    private val scope: CoroutineScope,
    private val webViewDestination: WebViewDestination,
) : AnimePlayerNavigation {
    private val navigator get() = visit.navigator

    override val webViewAvailable: Boolean get() = webViewDestination.isAvailable

    override fun close() = player.close(visit)

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

    override fun openEntry(entryId: Long) = takePlayersPlace(EntryScreen(entryId))

    override fun openSourceSettings(sourceId: Long) = takePlayersPlace(SourcePreferencesScreen(sourceId))

    override fun openWebView(url: String, title: String?, sourceId: Long?) {
        navigator.push(webViewDestination.screen(url, title, sourceId, headers = emptyMap()))
    }

    override fun enterPictureInPicture() = player.enterPictureInPicture(visit)

    override fun bringToMainWindow() = player.bringToMainWindow(visit)

    private fun takePlayersPlace(screen: Screen) {
        if (navigator.lastItem === visit.screen) navigator.replace(screen) else navigator.push(screen)
    }
}
