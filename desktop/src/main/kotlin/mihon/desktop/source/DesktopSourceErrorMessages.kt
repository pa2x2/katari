package mihon.desktop.source

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.SourceErrorMessages
import eu.kanade.presentation.util.platformGapSourceMessage
import mihon.core.platform.PlatformContext
import mihon.extension.runtime.jvm.diagnostics.PlatformGaps

/**
 * A source's error on desktop: what it needed that desktop doesn't provide yet, when that's why it failed, and otherwise
 * its type and message, as Android shows an error it doesn't recognise.
 */
@Inject
class DesktopSourceErrorMessages : SourceErrorMessages {
    override fun message(context: PlatformContext, error: Throwable): String {
        PlatformGaps.missingMember(error)?.let { return context.platformGapSourceMessage(it) }
        return when (val className = error::class.simpleName) {
            "Exception", "IOException" -> error.message ?: className
            else -> "$className: ${error.message}"
        }
    }
}
