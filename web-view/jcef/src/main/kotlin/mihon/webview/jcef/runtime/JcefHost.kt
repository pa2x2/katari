package mihon.webview.jcef.runtime

import java.nio.file.Path

/** What Chromium needs to know of the desktop the app runs on, answered by the app's desktop layer. */
interface JcefHost {
    /**
     * The display system the app's windows go through, which Chromium must use too; null where the windows weren't
     * set up to host Chromium, which then doesn't start.
     */
    val display: JcefDisplay?

    /** Physical pixels per CSS pixel: the session's scale, which Chromium takes once, as it starts. */
    val deviceScaleFactor: Double

    /** The folder Chromium keeps its profile and caches in, which the app may clear. */
    val dataDirectory: Path
}

enum class JcefDisplay {
    /** X11, which a Wayland session reaches through XWayland. */
    X11,

    WAYLAND,
}
