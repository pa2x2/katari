package app.katari.desktop.display.backend

import app.katari.presentation.kit.pointer.TouchpadPinch
import kotlinx.coroutines.flow.Flow
import java.awt.Frame
import java.awt.Window

/** How the app's windows reach the display, set up once, at start, as the window toolkit is fixed once AWT starts. */
interface DisplayBackend {
    /** Sets the runtime up for the app's windows. Must run before anything starts AWT. */
    fun start()

    /**
     * Brings [window] forward, restoring it when minimised, for a later launch of the app, with the
     * [activationToken] the desktop gave that launch. Where the desktop's focus rules don't let it take the focus,
     * the desktop shows that it wants attention instead.
     */
    fun bringForward(window: Frame, activationToken: String?)

    /**
     * The touchpad pinches over [window], a shown window, while collected; null where the system's libraries can't
     * read them. One collection at a time per window: share it.
     */
    fun touchpadPinches(window: Window): Flow<TouchpadPinch>?
}
