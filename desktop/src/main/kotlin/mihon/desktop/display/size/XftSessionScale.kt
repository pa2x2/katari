package mihon.desktop.display.size

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.awt.Toolkit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Linux: on X11, and XWayland, AWT draws in physical pixels and scales only by whole numbers, so a scale such as 1.25
 * is left to the app. X11 sessions publish their scale as `Xft.dpi` in the X resources, 96 being 1:1, which KDE,
 * GNOME, XFCE and setups loading `~/.Xresources` set; it is read once with `xrdb`, or from the `Xft/DPI` XSETTINGS
 * AWT reads where xrdb isn't installed. Where neither is published, AWT's own scale stays.
 */
class XftSessionScale : SessionScale {
    override val pixelsPerDp: Float? by lazy {
        val dpi = resourcesDpi() ?: xsettingsDpi()
        if (dpi == null) logcat(LogPriority.DEBUG) { "The X session publishes no Xft.dpi" }
        dpi?.takeIf { it > 0f }?.div(UNSCALED_DPI)
    }

    private fun resourcesDpi(): Float? {
        val query = try {
            ProcessBuilder("xrdb", "-query")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: IOException) {
            logcat(LogPriority.DEBUG, e) { "Can't read the X resources through xrdb" }
            return null
        }
        val output = query.inputStream.bufferedReader().use { it.readText() }
        if (!query.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) query.destroy()
        return XFT_DPI.find(output)?.groupValues?.get(1)?.toFloatOrNull()
    }

    // XSETTINGS give the DPI in 1024ths
    private fun xsettingsDpi(): Float? =
        (Toolkit.getDefaultToolkit().getDesktopProperty("gnome.Xft/DPI") as? Int)?.div(XSETTINGS_DPI_UNIT)

    private companion object {
        const val UNSCALED_DPI = 96f
        const val XSETTINGS_DPI_UNIT = 1024f
        const val TIMEOUT_SECONDS = 2L
        val XFT_DPI = Regex("""(?m)^Xft\.dpi:\s*([\d.]+)""")
    }
}
