package app.katari.desktop.webview

import app.katari.desktop.application.DesktopBuildInfo
import app.katari.desktop.display.size.SessionScale
import app.katari.desktop.storage.DesktopDirectories
import app.katari.webview.jcef.runtime.JcefHost
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.network.NetworkPreferences
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.nio.file.Path

/**
 * Chromium's view of the desktop and the app: the session's scale, the app's user agent, its cache folder for
 * Chromium's own data, and its data folder for a Chromium it installs.
 */
@Inject
class DesktopJcefHost(
    private val sessionScale: SessionScale,
    private val directories: DesktopDirectories,
    private val networkPreferences: NetworkPreferences,
) : JcefHost {
    // Where the session leaves no scale to the app, the windows' own scale is the session's
    override val deviceScaleFactor: Double
        get() = sessionScale.pixelsPerDp?.toDouble()
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
                .defaultTransform.scaleX

    override val userAgent: String
        get() = networkPreferences.defaultUserAgent.get().trim()

    // Named for its version, so an app that needs another Chromium never takes this one for it
    override val chromiumDirectory: Path
        get() = directories.data.resolve(CHROMIUM_DIRECTORY_NAME).resolve(DesktopBuildInfo.CHROMIUM_VERSION)

    override val dataDirectory: Path
        get() = Files.createDirectories(directories.cache.resolve(DATA_DIRECTORY_NAME))

    private companion object {
        const val DATA_DIRECTORY_NAME = "web-view"
        const val CHROMIUM_DIRECTORY_NAME = "chromium"
    }
}
