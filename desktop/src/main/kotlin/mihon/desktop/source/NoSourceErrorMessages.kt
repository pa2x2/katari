package mihon.desktop.source

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.SourceErrorMessages
import mihon.core.platform.PlatformContext

/**
 * Desktop has no sources yet, whose failures are the ones worth wording, so an error is shown as Android shows one it
 * doesn't recognise: its type and message. Stands in until desktop hosts extensions.
 */
@Inject
class NoSourceErrorMessages : SourceErrorMessages {
    override fun message(context: PlatformContext, error: Throwable): String =
        when (val className = error::class.simpleName) {
            "Exception", "IOException" -> error.message ?: className
            else -> "$className: ${error.message}"
        }
}
