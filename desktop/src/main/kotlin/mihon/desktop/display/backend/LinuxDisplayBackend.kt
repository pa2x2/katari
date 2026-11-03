package mihon.desktop.display.backend

import logcat.LogPriority
import mihon.desktop.display.x11.X11LaunchHandover
import mihon.desktop.display.x11.X11WindowClass
import tachiyomi.core.common.util.system.logcat
import java.awt.Frame

/**
 * Linux: AWT on X11, which a Wayland session runs through XWayland, with Compose drawing on the GPU; or, in a Wayland
 * session on the JetBrains Runtime, its native Wayland toolkit (`WLToolkit`), which gives Compose's window no drawing
 * surface, so Compose draws through Swing's graphics on the CPU. Other runtimes have no Wayland toolkit.
 *
 * Either way the windows are named after [applicationId], as the app's desktop entry is, so the desktop shows them as
 * the app's: the native toolkit gives it as the windows' Wayland app ID, X11 as their `WM_CLASS`.
 */
class LinuxDisplayBackend(private val applicationId: String) : DisplayBackend {
    override val isNativeWaylandOffered: Boolean by lazy { isWaylandSession() && hasWaylandToolkit() }

    override var windowDrawing: WindowDrawing = WindowDrawing.Accelerated
        private set

    override var isNativeWayland: Boolean = false
        private set

    override fun start(preferNativeWayland: Boolean) {
        if (preferNativeWayland && isNativeWaylandOffered) {
            System.setProperty("awt.toolkit.name", WAYLAND_TOOLKIT)
            // The renderer the native toolkit was measured with: Skia rasterises on the CPU into Swing's image
            System.setProperty("skiko.renderApi", "SOFTWARE")
            System.setProperty("awt.app.id", applicationId)
            isNativeWayland = true
            windowDrawing = WindowDrawing.SwingGraphics
        } else {
            X11WindowClass.set(applicationId)
        }
        logcat(LogPriority.INFO) {
            if (isNativeWayland) "Windows go through native Wayland" else "Windows go through X11"
        }
    }

    /**
     * On X11, the launch's token goes to the window manager as the window's startup id, which tells it that the click
     * that started the launch asks for the window. The native Wayland toolkit takes no token from elsewhere, so there
     * the window asks the compositor on its own, which a compositor may refuse to a window the user didn't just use.
     */
    override fun bringForward(window: Frame, activationToken: String?) {
        if (window.extendedState and Frame.ICONIFIED != 0) {
            window.extendedState = window.extendedState and Frame.ICONIFIED.inv()
        }
        if (!isNativeWayland && activationToken != null) X11LaunchHandover.handOver(window, activationToken)
        window.toFront()
    }

    private fun isWaylandSession(): Boolean = !System.getenv("WAYLAND_DISPLAY").isNullOrEmpty()

    // Looked up without initialising it, which would start AWT
    private fun hasWaylandToolkit(): Boolean = try {
        Class.forName("sun.awt.wl.$WAYLAND_TOOLKIT", false, null)
        true
    } catch (_: ClassNotFoundException) {
        logcat(LogPriority.DEBUG) { "This Java runtime has no native Wayland toolkit" }
        false
    }

    private companion object {
        const val WAYLAND_TOOLKIT = "WLToolkit"
    }
}
