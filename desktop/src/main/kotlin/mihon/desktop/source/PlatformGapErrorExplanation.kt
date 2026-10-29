package mihon.desktop.source

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.SourceErrorExplanation
import eu.kanade.presentation.util.platformGapSourceMessage
import mihon.core.platform.PlatformContext
import mihon.extension.runtime.jvm.diagnostics.PlatformGaps

/** A source's call that failed because desktop doesn't provide yet what it needed of Android, named. */
@Inject
class PlatformGapErrorExplanation : SourceErrorExplanation {
    override fun explain(context: PlatformContext, error: Throwable): String? =
        PlatformGaps.missingMember(error)?.let(context::platformGapSourceMessage)
}
