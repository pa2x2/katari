package mihon.desktop.player.pictureinpicture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.application.icon.AppIcon
import mihon.desktop.window.WindowPreferences
import mihon.entry.interactions.player.AnimePlayerPictureInPictureWindow
import mihon.presentation.kit.shortcut.KeyboardShortcutRegistry
import mihon.presentation.kit.shortcut.KeyboardShortcutsHost
import tachiyomi.i18n.MR
import tachiyomi.i18n.window_title_with_screen
import tachiyomi.presentation.core.i18n.stringResource
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import kotlin.math.roundToInt

/**
 * The picture-in-picture window as a Compose window of the app's: undecorated, so it has no title bar, and asking to
 * stay over the other windows, which X11 window managers do. It moves and resizes itself ([movesWindow],
 * [ResizeEdges]), as a window without a title bar or frame gets nothing from the window manager to do either with.
 * Its place and size are saved as they change and come back the next time it opens.
 */
internal class ComposePictureInPictureWindow(
    private val preferences: WindowPreferences,
) : AnimePlayerPictureInPictureWindow {
    @Composable
    override fun Window(
        title: String,
        aspectRatio: Float?,
        onDoubleClick: () -> Unit,
        onCloseRequest: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val shape = pictureInPictureShape(aspectRatio)
        val state = remember { initialState(shape) }
        val shortcuts = remember { KeyboardShortcutRegistry() }
        val icon = remember { AppIcon.painter() }
        LaunchedEffect(state) {
            snapshotFlow { state.position to state.size }
                .collectLatest { (position, size) ->
                    if (position !is WindowPosition.Absolute) return@collectLatest
                    delay(SAVE_DELAY_MS)
                    preferences.pictureInPictureBounds.set(
                        Rectangle(
                            position.x.value.roundToInt(),
                            position.y.value.roundToInt(),
                            size.width.value.roundToInt(),
                            size.height.value.roundToInt(),
                        ),
                    )
                }
        }
        Window(
            onCloseRequest = onCloseRequest,
            state = state,
            title = stringResource(MR.strings.window_title_with_screen, DesktopAppVariant.current.displayName, title),
            icon = icon,
            undecorated = true,
            // It resizes itself, keeping the video's shape; resizable, Compose would let any edge stretch it
            resizable = false,
            alwaysOnTop = true,
            onKeyEvent = shortcuts::onKeyEvent,
        ) {
            // The video's shape known or changed after the window opened, as for the next episode
            LaunchedEffect(window, shape) {
                val reshaped = reshapedPictureInPictureBounds(window.bounds, shape)
                if (reshaped != window.bounds) window.bounds = reshaped
            }
            KeyboardShortcutsHost(shortcuts) {
                Box(modifier = Modifier.fillMaxSize().movesWindow(window, onDoubleClick)) {
                    content()
                    ResizeEdges(window, shape)
                }
            }
        }
    }

    // Compose places desktop windows in AWT's units, one dp each
    private fun initialState(shape: Float): WindowState {
        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = environment.screenDevices.map { it.defaultConfiguration.bounds }
        val bounds = preferences.pictureInPictureBounds.get()
            ?.takeIf { screens.isNotEmpty() }
            ?.let { restoredPictureInPictureBounds(it, shape, screens) }
            ?: firstPictureInPictureBounds(environment.maximumWindowBounds, shape)
        return WindowState(
            position = WindowPosition(bounds.x.dp, bounds.y.dp),
            size = DpSize(bounds.width.dp, bounds.height.dp),
        )
    }

    private companion object {
        // Moving and resizing change the bounds for every pointer move; only where the window comes to rest is kept
        const val SAVE_DELAY_MS = 500L
    }
}
