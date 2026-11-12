package app.katari.desktop.presence

import app.katari.desktop.window.MainWindowVisibility
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the user has left the app, which starts the clock of a profile's "Lock when idle", as Android's app going to
 * the background does: the main window is minimized or hidden in the tray while no picture-in-picture window is open,
 * or the screen is locked. A window that is only covered by others still counts as in view, since a desktop doesn't
 * tell.
 */
@SingleIn(AppScope::class)
@Inject
class AppPresence(screenLock: ScreenLock, mainWindow: MainWindowVisibility) {
    private val mainWindowMinimized = MutableStateFlow(false)
    private val pictureInPictureOpen = MutableStateFlow(false)

    val isAway: Flow<Boolean> = combine(
        mainWindowMinimized,
        mainWindow.isHidden,
        pictureInPictureOpen,
        screenLock.isLocked,
    ) { minimized, hidden, pictureInPicture, locked ->
        locked || ((minimized || hidden) && !pictureInPicture)
    }.distinctUntilChanged()

    fun mainWindowMinimized(minimized: Boolean) {
        mainWindowMinimized.value = minimized
    }

    fun pictureInPictureOpen(open: Boolean) {
        pictureInPictureOpen.value = open
    }
}
