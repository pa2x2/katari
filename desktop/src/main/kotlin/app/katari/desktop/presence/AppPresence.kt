package app.katari.desktop.presence

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the user has left the app, which starts the clock of a profile's "Lock when idle", as Android's app going to
 * the background does: the main window is minimized while no picture-in-picture window is open, or the screen is
 * locked. A window that is only covered by others still counts as in view, since a desktop doesn't tell. Whether the
 * main window is minimized also decides whether the app shows for sync ([app.katari.ui.host.AppVisibility]).
 */
@SingleIn(AppScope::class)
@Inject
class AppPresence(screenLock: ScreenLock) {
    private val mainWindowMinimized = MutableStateFlow(false)
    private val pictureInPictureOpen = MutableStateFlow(false)

    val isMainWindowMinimized: StateFlow<Boolean> = mainWindowMinimized

    val isAway: Flow<Boolean> = combine(
        mainWindowMinimized,
        pictureInPictureOpen,
        screenLock.isLocked,
    ) { minimized, pictureInPicture, locked ->
        locked || (minimized && !pictureInPicture)
    }.distinctUntilChanged()

    fun mainWindowMinimized(minimized: Boolean) {
        mainWindowMinimized.value = minimized
    }

    fun pictureInPictureOpen(open: Boolean) {
        pictureInPictureOpen.value = open
    }
}
