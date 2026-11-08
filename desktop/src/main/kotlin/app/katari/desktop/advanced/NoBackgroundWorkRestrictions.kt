package app.katari.desktop.advanced

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.advanced.BackgroundWorkRestrictions
import eu.kanade.presentation.more.settings.screen.advanced.UnrestrictedBackgroundWorkRequest

/** Desktop systems don't restrict an app's background work the way Android's battery optimisation does. */
@Inject
class NoBackgroundWorkRestrictions : BackgroundWorkRestrictions {
    override val isOffered: Boolean = false

    override fun requestUnrestricted(context: PlatformContext): UnrestrictedBackgroundWorkRequest =
        UnrestrictedBackgroundWorkRequest.AlreadyUnrestricted
}
