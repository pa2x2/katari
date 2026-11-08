package mihon.desktop.display.backend

import java.awt.Frame

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
}
