package eu.kanade.tachiyomi.ui.video.player.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.katari.entry.viewer.chrome.ui.window.ViewerFullScreen
import app.katari.presentation.kit.window.WindowFullScreen

/**
 * The player's full screen: what [ViewerFullScreen] does for every viewer, and moving into picture-in-picture leaves
 * full screen, as the main window then shows other screens, while coming back to the main window enters it again if
 * the player was in it (D022). Episode changes keep full screen, as the player stays open through them.
 */
@Composable
internal fun VideoPlayerFullScreen(
    fullScreen: WindowFullScreen?,
    inPictureInPicture: Boolean,
    content: @Composable () -> Unit,
) {
    if (fullScreen != null) {
        var fullScreenBeforePictureInPicture by remember(fullScreen) { mutableStateOf(false) }
        LaunchedEffect(fullScreen, inPictureInPicture) {
            if (inPictureInPicture) {
                fullScreenBeforePictureInPicture = fullScreen.isFullScreen
                fullScreen.leave()
            } else if (fullScreenBeforePictureInPicture) {
                fullScreenBeforePictureInPicture = false
                fullScreen.enter()
            }
        }
    }
    ViewerFullScreen(fullScreen, content)
}
