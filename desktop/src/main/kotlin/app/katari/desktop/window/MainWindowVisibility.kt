package app.katari.desktop.window

import app.katari.desktop.display.backend.DisplayBackend
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app's window is hidden, as closing it hides it while the app keeps running in the tray, and the one way
 * everything that wants the window in front brings it there: a later launch, a notification, the tray, the player.
 */
@SingleIn(AppScope::class)
@Inject
class MainWindowVisibility(private val openWindow: OpenWindow, private val displayBackend: DisplayBackend) {
    private val hidden = MutableStateFlow(false)

    val isHidden: StateFlow<Boolean> = hidden.asStateFlow()

    /** Whether the app started hidden in the tray ([startInTray]), as at login, before the window was ever shown. */
    var startedInTray: Boolean = false
        private set

    /** Starts the app with the window hidden in the tray. Before the window composes. */
    fun startInTray() {
        startedInTray = true
        hidden.value = true
    }

    /** Hides the window, which [KatariWindow] does once it next composes. */
    fun hide() {
        hidden.value = true
    }

    /**
     * Shows the window if it is hidden and brings it forward through [DisplayBackend.bringForward] with
     * [activationToken]. On the window's thread.
     */
    fun bringForward(activationToken: String?) {
        val window = openWindow.context?.window ?: return
        // At once rather than once the window composes, as the window manager only brings forward a shown window
        window.isVisible = true
        hidden.value = false
        displayBackend.bringForward(window, activationToken)
    }
}
