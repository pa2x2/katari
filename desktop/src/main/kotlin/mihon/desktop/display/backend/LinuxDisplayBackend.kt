package mihon.desktop.display.backend

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Linux: AWT on X11, which a Wayland session runs through XWayland, with Compose drawing on the GPU; or, in a Wayland
 * session on the JetBrains Runtime, its native Wayland toolkit (`WLToolkit`), which gives Compose's window no drawing
 * surface, so Compose draws through Swing's graphics on the CPU. Other runtimes have no Wayland toolkit.
 */
class LinuxDisplayBackend : DisplayBackend {
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
            isNativeWayland = true
            windowDrawing = WindowDrawing.SwingGraphics
        }
        logcat(LogPriority.INFO) {
            if (isNativeWayland) "Windows go through native Wayland" else "Windows go through X11"
        }
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
