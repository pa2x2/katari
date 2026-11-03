package mihon.desktop.link

import logcat.LogPriority
import mihon.core.platform.PlatformContext
import mihon.core.platform.link.LinkOpener
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.open_link_error
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Linux, through `xdg-open`, which hands the link to the handler the user's desktop has set for it; the XDG OpenURI
 * portal, which also works inside a sandbox such as Flatpak, replaces it later. Katari claims no web links on desktop,
 * so [open]'s `forceDefaultBrowser` needs nothing here. A link that can't be opened is told in [messages].
 */
class XdgLinkOpener(private val messages: AppMessages) : LinkOpener {
    override fun open(context: PlatformContext, url: String, forceDefaultBrowser: Boolean) {
        // Parsed first so that nothing but an absolute URI, such as an argument xdg-open would take as an option, is
        // passed on.
        val uri = try {
            URI(url).takeIf(URI::isAbsolute)
        } catch (e: URISyntaxException) {
            null
        }
        if (uri == null) {
            logcat(LogPriority.WARN) { "Not opening $url: not an absolute URI" }
            showFailure(url)
            return
        }
        val process = try {
            ProcessBuilder("xdg-open", uri.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not start xdg-open for $uri" }
            showFailure(url)
            return
        }
        // xdg-open can wait for the handler it falls back to, such as a terminal browser, so its exit is not awaited.
        process.onExit().thenAccept { exited ->
            if (exited.exitValue() != 0) {
                logcat(LogPriority.ERROR) { "xdg-open exited with ${exited.exitValue()} for $uri" }
                showFailure(url)
            }
        }
    }

    private fun showFailure(url: String) {
        messages.show(stringResource(MR.strings.open_link_error, url))
    }
}
