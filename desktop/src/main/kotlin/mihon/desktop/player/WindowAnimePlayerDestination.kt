package mihon.desktop.player

import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.core.platform.PlatformContext
import mihon.desktop.window.WindowPlatformContext
import mihon.entry.interactions.player.AnimePlayer
import mihon.entry.interactions.player.AnimePlayerDestination
import mihon.entry.interactions.player.AnimePlayerRequest

/**
 * The player as a screen of the window's navigation, as the web view is. One player shows at a time: opening an
 * episode while one is open, as from a notification, takes its place along with anything opened over it.
 */
class WindowAnimePlayerDestination(
    private val player: AnimePlayer,
    private val webViewDestination: WebViewDestination,
) : AnimePlayerDestination {
    override fun open(context: PlatformContext, request: AnimePlayerRequest) {
        val navigator = (context as? WindowPlatformContext)?.navigator
            ?: error("The player opens only from a window that shows the app's screens")
        val screen = AnimePlayerScreen(request, player, webViewDestination)
        if (navigator.items.any { it is AnimePlayerScreen }) {
            navigator.popUntil { it is AnimePlayerScreen }
            navigator.replace(screen)
        } else {
            navigator.push(screen)
        }
    }
}
