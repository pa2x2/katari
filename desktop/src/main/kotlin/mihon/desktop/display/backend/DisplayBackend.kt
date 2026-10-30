package mihon.desktop.display.backend

import java.awt.Frame

/**
 * How the app's windows reach the display. The window toolkit is fixed once anything starts AWT, so the backend is
 * chosen once, at start.
 */
interface DisplayBackend {
    /**
     * Whether the session can be reached through the runtime's native Wayland toolkit instead of the default, which
     * "Use native Wayland" picks; false in X11 sessions, on other platforms and on runtimes without the toolkit.
     */
    val isNativeWaylandOffered: Boolean

    /** How this run's windows draw; [WindowDrawing.Accelerated] until [start] picks otherwise. */
    val windowDrawing: WindowDrawing

    /** Whether this run's windows go through the native Wayland toolkit. */
    val isNativeWayland: Boolean

    /** Whether [start] set the display connection up for a web view to share it; false until then. */
    val isPreparedForWebView: Boolean

    /**
     * Sets the runtime up for the native Wayland toolkit where [preferNativeWayland] asks for it and it is offered,
     * and for the default otherwise, and for a web view where [prepareForWebView] asks, as only a run that may show one
     * should. Must run before anything starts AWT.
     */
    fun start(preferNativeWayland: Boolean, prepareForWebView: Boolean)

    /**
     * Brings [window] forward, restoring it when minimised, for a later launch of the app, with the
     * [activationToken] the desktop gave that launch. Where the desktop's focus rules don't let it take the focus,
     * the desktop shows that it wants attention instead.
     */
    fun bringForward(window: Frame, activationToken: String?)
}

enum class WindowDrawing {
    /** Compose's own window, which draws on the GPU where it can. */
    Accelerated,

    /**
     * A Swing frame Compose draws into through Swing's graphics on the CPU, for toolkits Compose's own window can't
     * draw on: it needs a native drawing surface those toolkits don't give.
     */
    SwingGraphics,
}
