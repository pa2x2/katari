package app.katari.desktop.webview

import app.katari.desktop.display.size.SessionScale
import app.katari.desktop.storage.DesktopDirectories
import app.katari.webview.jcef.runtime.JcefHost
import dev.zacsweers.metro.Inject
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.nio.file.Path

/** Chromium's view of the desktop: the session's scale and the app's cache folder. */
@Inject
class DesktopJcefHost(
    private val sessionScale: SessionScale,
    private val directories: DesktopDirectories,
) : JcefHost {
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
