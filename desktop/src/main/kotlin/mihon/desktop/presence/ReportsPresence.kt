package mihon.desktop.presence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import mihon.desktop.player.WindowAnimePlayer
import java.awt.Frame
import java.awt.event.WindowStateListener
import javax.swing.JFrame

/** Tells [presence] whether the main [window] is minimized and whether [player] shows in a picture-in-picture window. */
@Composable
internal fun ReportsPresence(window: JFrame, player: WindowAnimePlayer, presence: AppPresence) {
    DisposableEffect(window) {
        val listener = WindowStateListener { event ->
            presence.mainWindowMinimized(event.newState and Frame.ICONIFIED != 0)
        }
        window.addWindowStateListener(listener)
        presence.mainWindowMinimized(window.extendedState and Frame.ICONIFIED != 0)
        onDispose { window.removeWindowStateListener(listener) }
    }
    LaunchedEffect(player) {
        snapshotFlow { player.visit?.inPictureInPicture == true }.collect(presence::pictureInPictureOpen)
    }
}
