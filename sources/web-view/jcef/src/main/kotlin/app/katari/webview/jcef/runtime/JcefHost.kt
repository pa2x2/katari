package app.katari.webview.jcef.runtime

import java.nio.file.Path

/** What Chromium needs to know of the desktop the app runs on, answered by the app's desktop layer. */
interface JcefHost {
    /** Physical pixels per CSS pixel: the session's scale, which Chromium takes once, as it starts. */
    val deviceScaleFactor: Double

    /**
     * The user agent the app's requests send, which Chromium takes once, as it starts, for what no page sets one on:
     * service workers, which would otherwise tell a site Chromium's own.
     */
    val userAgent: String

    /**
     * The folder the app installs Chromium to, for a Java runtime that carries JCEF without one. It may not exist; a
     * Chromium is there whole or not at all.
     */
    val chromiumDirectory: Path

    /** The folder Chromium keeps its profile and caches in, which the app may clear. */
    val dataDirectory: Path
}
