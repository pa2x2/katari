package eu.kanade.tachiyomi.ui.video.player.window

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerHintPill
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import mihon.presentation.kit.window.WindowFullScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What full screen does while the player is open: entering it says for a moment that Esc leaves it, and going back
 * leaves full screen before it leaves the player. Anything opened over the picture, such as the settings panel, closes
 * first, as its own back handler comes after this one. Leaving the player puts the window back as it was when the
 * player opened, so full screen the player entered ends with it, while episode changes keep it. Moving into
 * picture-in-picture leaves full screen, as the main window then shows other screens, and coming back to the main
 * window enters it again if the player was in it (D022).
 */
@Composable
internal fun VideoPlayerFullScreen(
    fullScreen: WindowFullScreen?,
    inPictureInPicture: Boolean,
    content: @Composable () -> Unit,
) {
    if (fullScreen == null) {
        content()
        return
    }
    val fullScreenAtOpen = remember(fullScreen) { fullScreen.isFullScreen }
    DisposableEffect(fullScreen) {
        onDispose { if (!fullScreenAtOpen) fullScreen.leave() }
    }
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
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = fullScreen.isFullScreen,
        onBackCompleted = fullScreen::leave,
    )
    var hintVisible by remember { mutableStateOf(false) }
    LaunchedEffect(fullScreen) {
        snapshotFlow { fullScreen.isFullScreen }.drop(1).collectLatest { entered ->
            hintVisible = entered
            if (entered) {
                delay(HINT_DURATION_MS)
                hintVisible = false
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = hintVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            VideoPlayerHintPill(label = stringResource(MR.strings.anime_playback_full_screen_hint))
        }
    }
}

private const val HINT_DURATION_MS = 2_000L
