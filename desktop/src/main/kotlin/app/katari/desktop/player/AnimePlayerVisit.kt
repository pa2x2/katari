package app.katari.desktop.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.entry.interactions.player.AnimePlayerRequest
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.base.lifecycle.DesktopScreenLifecycleOwner

/**
 * One visit to the player, from opening [request]'s episode, from a screen of [navigator], until the player closes.
 * Its ViewModels and lifecycle are its own ([lifecycle]) rather than its screen's, since it leaves the navigation while
 * it plays in picture-in-picture.
 */
internal class AnimePlayerVisit(val request: AnimePlayerRequest, val navigator: Navigator) {
    /** The player's screen in [navigator] while the player is in the main window; null in picture-in-picture. */
    var screen by mutableStateOf<AnimePlayerScreen?>(null)

    var inPictureInPicture by mutableStateOf(false)

    val lifecycle: DesktopScreenLifecycleOwner = DesktopScreenLifecycleOwner.detached()

    /** Takes the player's [screen] out of [navigator], wherever in it the screen is. */
    fun leaveNavigation() {
        val screen = screen ?: return
        this.screen = null
        when {
            navigator.lastItem === screen -> navigator.pop()
            screen in navigator.items -> navigator.replaceAll(navigator.items.filter { it !== screen })
        }
    }
}
