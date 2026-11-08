package app.katari.desktop.source

import app.katari.core.platform.PlatformContext
import app.katari.extension.runtime.jvm.diagnostics.PlatformGaps
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.SourceErrorExplanation
import eu.kanade.presentation.util.platformGapSourceMessage

/** A source's call that failed because desktop doesn't provide yet what it needed of Android, named. */
@Inject
class PlatformGapErrorExplanation : SourceErrorExplanation {
    override fun explain(context: PlatformContext, error: Throwable): String? =
        PlatformGaps.missingMember(error)?.let(context::platformGapSourceMessage)
}
