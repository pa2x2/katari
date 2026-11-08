package app.katari.entry.viewer.chrome.ui.window

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.window.WindowFullScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What full screen does while a viewer such as the reader or the player is open: entering it says for a moment that
 * Esc leaves it, and going back leaves full screen before it leaves the viewer. Anything opened over the viewer, such
 * as a settings panel, closes first, as its own back handler comes after this one. Leaving the viewer puts the window
 * back as it was when the viewer opened, so full screen the viewer entered ends with it. Without a [fullScreen], as on
 * Android, this is only the [content].
 */
@Composable
fun ViewerFullScreen(
    fullScreen: WindowFullScreen?,
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
            FullScreenHint()
        }
    }
}

@Composable
private fun FullScreenHint() {
    val spacing = KatariTheme.spacing
    OverlayPill(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(top = spacing.xl, start = spacing.l, end = spacing.l),
        style = OverlayStyle.Scrim,
    ) {
        Text(
            text = stringResource(MR.strings.viewer_full_screen_hint),
            role = TextRole.Body,
            modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.s),
        )
    }
}

private const val HINT_DURATION_MS = 2_000L
