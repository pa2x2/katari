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

/**
 * Linux, through `xdg-open`, which hands the link to the handler the user's desktop has set for it; the XDG OpenURI
 * portal, which also works inside a sandbox such as Flatpak, replaces it later. Katari claims no web links on desktop,
 * so [open]'s `forceDefaultBrowser` needs nothing here. A link that can't be opened is told in [messages].
 */
class XdgLinkOpener(private val messages: AppMessages) : LinkOpener {
    override fun open(context: PlatformContext, url: String, forceDefaultBrowser: Boolean) {
        // Only an absolute URI, so that nothing xdg-open would take as an option is passed on. The link itself is passed
        // as given, as Android does: java.net.URI rejects characters that browsers escape themselves, such as the `|`
        // some extensions keep in episode links.
        if (!ABSOLUTE_URI.matches(url)) {
            logcat(LogPriority.WARN) { "Not opening $url: not an absolute URI" }
            showFailure(url)
            return
        }
        val process = try {
            ProcessBuilder("xdg-open", url)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not start xdg-open for $url" }
            showFailure(url)
            return
        }
        // xdg-open can wait for the handler it falls back to, such as a terminal browser, so its exit is not awaited.
        process.onExit().thenAccept { exited ->
            if (exited.exitValue() != 0) {
                logcat(LogPriority.ERROR) { "xdg-open exited with ${exited.exitValue()} for $url" }
                showFailure(url)
            }
        }
    }

    private fun showFailure(url: String) {
        messages.show(stringResource(MR.strings.open_link_error, url))
    }

    private companion object {
        // RFC 3986 scheme, then the rest of the link with no whitespace
        val ABSOLUTE_URI = Regex("""[A-Za-z][A-Za-z0-9+.-]*:\S+""")
    }
}
