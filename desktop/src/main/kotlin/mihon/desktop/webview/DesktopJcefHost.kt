package mihon.desktop.webview

import dev.zacsweers.metro.Inject
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.display.size.SessionScale
import mihon.desktop.storage.DesktopDirectories
import mihon.webview.jcef.runtime.JcefDisplay
import mihon.webview.jcef.runtime.JcefHost
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.nio.file.Path

/** Chromium's view of the desktop: the app's display backend, the session's scale and the app's cache folder. */
@Inject
class DesktopJcefHost(
    private val backend: DisplayBackend,
    private val sessionScale: SessionScale,
    private val directories: DesktopDirectories,
) : JcefHost {
    override val display: JcefDisplay
        get() = if (backend.isNativeWayland) JcefDisplay.WAYLAND else JcefDisplay.X11

    // Where the session leaves no scale to the app, the windows' own scale is the session's
    override val deviceScaleFactor: Double
        get() = sessionScale.pixelsPerDp?.toDouble()
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
                .defaultTransform.scaleX

    override val dataDirectory: Path
        get() = Files.createDirectories(directories.cache.resolve(DATA_DIRECTORY_NAME))

    private companion object {
        const val DATA_DIRECTORY_NAME = "web-view"
    }
}
