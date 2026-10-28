package eu.kanade.presentation.util

import dev.zacsweers.metro.Inject
import mihon.core.platform.PlatformContext

@Inject
class AndroidSourceErrorMessages : SourceErrorMessages {
    override fun message(context: PlatformContext, error: Throwable): String = with(context) { error.formattedMessage }
}
