package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import kotlin.math.roundToInt

/**
 * The window's state, opened where [preferences] last saw it and saved there as it changes. A window saved on no
 * screen there is now opens where the window manager puts it, and the first start opens a window sized to the main
 * screen. Compose places desktop windows in AWT's units, one dp each.
 */
@Composable
fun rememberSavedWindowState(preferences: WindowPreferences): WindowState {
    val state = remember(preferences) {
        val saved = preferences.floatingBounds.get()
        val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .map { it.defaultConfiguration.bounds }
        val size = saved ?: firstStartSize()
        WindowState(
            placement = if (preferences.maximized.get()) WindowPlacement.Maximized else WindowPlacement.Floating,
            position = if (saved != null && isReachable(saved, screens)) {
                WindowPosition(saved.x.dp, saved.y.dp)
            } else {
                WindowPosition.PlatformDefault
            },
            size = DpSize(size.width.dp, size.height.dp),
        )
    }
    LaunchedEffect(state) {
        snapshotFlow { Triple(state.placement, state.position, state.size) }.collect { (placement, position, size) ->
            if (state.isMinimized) return@collect
            preferences.maximized.set(placement == WindowPlacement.Maximized)
            // A maximised or full screen window covers the screen; the floating place is kept for when it's restored
            if (placement == WindowPlacement.Floating && position.isSpecified) {
                preferences.floatingBounds.set(
                    Rectangle(
                        position.x.value.roundToInt(),
                        position.y.value.roundToInt(),
                        size.width.value.roundToInt(),
                        size.height.value.roundToInt(),
                    ),
                )
            }
        }
    }
    return state
}

private fun firstStartSize(): Rectangle {
    val usable = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    return Rectangle(
        (usable.width * FIRST_START_SCREEN_SHARE).roundToInt(),
        (usable.height * FIRST_START_SCREEN_SHARE).roundToInt(),
    )
}

private const val FIRST_START_SCREEN_SHARE = 0.75f
