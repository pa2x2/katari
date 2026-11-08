package app.katari.desktop.display.backend

import app.katari.desktop.display.x11.X11LaunchHandover
import app.katari.desktop.display.x11.X11PointerButtons
import app.katari.desktop.display.x11.X11TouchpadPinches
import app.katari.desktop.display.x11.X11WindowClass
import app.katari.presentation.kit.pointer.TouchpadPinch
import kotlinx.coroutines.flow.Flow
import java.awt.Frame
import java.awt.Window

/**
 * Linux: AWT on X11, which a Wayland session runs through XWayland, with Compose drawing on the GPU. The windows are
 * named after [applicationId] as their `WM_CLASS`, as the app's desktop entry is, so the desktop shows them as the
 * app's.
 */
class LinuxDisplayBackend(private val applicationId: String) : DisplayBackend {
    override fun start() {
        X11WindowClass.set(applicationId)
        X11PointerButtons.install()
    }

    /**
     * The launch's token goes to the window manager as the window's startup id, which tells it that the click that
     * started the launch asks for the window.
     */
    override fun bringForward(window: Frame, activationToken: String?) {
        if (window.extendedState and Frame.ICONIFIED != 0) {
            window.extendedState = window.extendedState and Frame.ICONIFIED.inv()
        }
        if (activationToken != null) X11LaunchHandover.handOver(window, activationToken)
        window.toFront()
    }

    override fun touchpadPinches(window: Window): Flow<TouchpadPinch>? = X11TouchpadPinches.over(window)
}
