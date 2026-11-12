package app.katari.desktop.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.core.platform.PlatformContext
import app.katari.desktop.window.MainWindowVisibility
import app.katari.desktop.window.WindowPlatformContext
import app.katari.entry.interactions.player.AnimePlayer
import app.katari.entry.interactions.player.AnimePlayerDestination
import app.katari.entry.interactions.player.AnimePlayerRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.webview.WebViewDestination

/**
 * The player in the app's window: one [visit] at a time, opened as a screen of the window's navigation that the
 * player fills ([AnimePlayerScreen]), while the player itself is composed by [Host] over the window's screens. So it
 * can leave the navigation for its picture-in-picture window, the main window going back to the screen under it, and
 * come back over whatever the main window shows by then, the same player and stream playing on throughout.
 *
 * Opening an episode while a visit is on, as from a notification, ends that visit, picture-in-picture or not, and the
 * new one takes its place along with anything opened over it.
 */
@SingleIn(AppScope::class)
@Inject
class WindowAnimePlayer(
    internal val player: AnimePlayer,
    internal val webViewDestination: WebViewDestination,
    internal val mainWindowVisibility: MainWindowVisibility,
) : AnimePlayerDestination {
    internal var visit by mutableStateOf<AnimePlayerVisit?>(null)
        private set

    override fun open(context: PlatformContext, request: AnimePlayerRequest) {
        val navigator = (context as? WindowPlatformContext)?.navigator
            ?: error("The player opens only from a window that shows the app's screens")
        val screen = AnimePlayerScreen()
        visit = AnimePlayerVisit(request, navigator).also { it.screen = screen }
        if (navigator.items.any { it is AnimePlayerScreen }) {
            navigator.popUntil { it is AnimePlayerScreen }
            navigator.replace(screen)
        } else {
            navigator.push(screen)
        }
    }

    internal fun enterPictureInPicture(visit: AnimePlayerVisit) {
        if (this.visit !== visit || visit.inPictureInPicture) return
        visit.inPictureInPicture = true
        visit.leaveNavigation()
    }

    internal fun bringToMainWindow(visit: AnimePlayerVisit) {
        if (this.visit !== visit) return
        if (visit.inPictureInPicture) {
            val screen = AnimePlayerScreen()
            visit.screen = screen
            visit.inPictureInPicture = false
            visit.navigator.push(screen)
        }
        mainWindowVisibility.bringForward(activationToken = null)
    }

    internal fun close(visit: AnimePlayerVisit) {
        if (this.visit !== visit) return
        this.visit = null
        visit.leaveNavigation()
    }
}
