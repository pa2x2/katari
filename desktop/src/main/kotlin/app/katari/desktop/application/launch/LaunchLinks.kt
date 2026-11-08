package app.katari.desktop.application.launch

import app.katari.desktop.home.HomeDestination
import app.katari.desktop.home.HomeDestinations
import eu.kanade.tachiyomi.ui.main.link.AppLink
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.net.URI
import java.net.URISyntaxException

/**
 * The links desktop takes from a launch, as the Android manifest's intent filters choose the links that reach the main
 * activity: those of the schemes the desktop entry registers the app for, and backup files. What each opens is
 * [AppLink]'s to say, as on Android. Any process of the user can hand a running instance links, so, as on Android, a
 * link only opens a screen: nothing is added or restored before the user confirms it there.
 */
internal object LaunchLinks {
    private const val FILE_SCHEME = "file"
    private val linkSchemes = setOf("katari", "tachiyomi", "app.katari")

    /** What [link], an absolute URI, opens, or null when desktop doesn't open it from a launch. */
    fun appLinkOf(link: String): AppLink? {
        val uri = try {
            URI(link)
        } catch (_: URISyntaxException) {
            return null
        }
        val appLink = AppLink.of(JavaUriLinkParts(uri)) ?: return null
        return when (uri.scheme) {
            FILE_SCHEME -> appLink.takeIf { it is AppLink.RestoreBackup }
            in linkSchemes -> appLink
            else -> null
        }
    }

    fun isFile(uri: URI): Boolean = uri.scheme == FILE_SCHEME

    /**
     * Opens each of [links] in [destinations], in order and each from the home's root, so the last one stays on
     * screen, as when Android's main activity gets several links in a row. Those this version doesn't open are logged.
     * On the window's thread.
     */
    suspend fun open(links: List<String>, destinations: HomeDestinations) {
        links.forEach { link ->
            val appLink = appLinkOf(link)
            if (appLink != null) {
                destinations.open(HomeDestination.Link(appLink))
            } else {
                logcat(LogPriority.WARN) { "Not opening $link: not a link or file Katari opens" }
            }
        }
    }
}
