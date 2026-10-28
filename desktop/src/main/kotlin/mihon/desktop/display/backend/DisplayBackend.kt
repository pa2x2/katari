package mihon.desktop.display.backend

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

    /**
     * Sets the runtime up for the native Wayland toolkit where [preferNativeWayland] asks for it and it is offered,
     * and for the default otherwise. Must run before anything starts AWT.
     */
    fun start(preferNativeWayland: Boolean)
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
