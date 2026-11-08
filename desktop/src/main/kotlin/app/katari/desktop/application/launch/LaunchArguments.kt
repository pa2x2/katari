package app.katari.desktop.application.launch

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path

/**
 * What a launch's command line asks the app to open. The desktop hands an app what it opens as arguments, such as the
 * desktop entry's `%U` from a browser or file manager, or a terminal's command line: each a link, or a file by its path
 * or `file:` URI.
 *
 * @property links the arguments [LaunchLinks] takes, in order, as absolute URIs. A file's path is resolved against
 * the launch's working directory here, since the running instance a later launch hands it to has its own, and its
 * URI takes the form the file chooser gives the screens.
 * @property ignored the arguments that are none of these, such as options, or that desktop doesn't open.
 */
internal class LaunchArguments(val links: List<String>, val ignored: List<String>) {
    /** Logs [ignored], once the log has started. */
    fun logIgnored() {
        ignored.forEach { logcat(LogPriority.WARN) { "Ignoring the argument $it" } }
    }

    companion object {
        private val uriScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

        fun read(arguments: List<String>, workingDirectory: Path): LaunchArguments {
            val links = arguments.map { it to linkOf(it, workingDirectory) }
            return LaunchArguments(
                links = links.mapNotNull { (_, link) -> link },
                ignored = links.filter { (_, link) -> link == null }.map { (argument, _) -> argument },
            )
        }

        private fun linkOf(argument: String, workingDirectory: Path): String? {
            if (argument.startsWith("-")) return null
            val link = try {
                if (uriScheme.containsMatchIn(argument)) {
                    val uri = URI(argument)
                    if (LaunchLinks.isFile(uri)) fileLink(Path.of(uri)) else argument
                } else {
                    fileLink(workingDirectory.resolve(argument))
                }
            } catch (_: URISyntaxException) {
                return null
            } catch (_: IllegalArgumentException) {
                // A path the system can't name, or a file URI with a host or a query
                return null
            }
            return link.takeIf { LaunchLinks.appLinkOf(it) != null }
        }

        private fun fileLink(path: Path): String = path.normalize().toUri().toString()
    }
}
