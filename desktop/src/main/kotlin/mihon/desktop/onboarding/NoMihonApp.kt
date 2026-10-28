package mihon.desktop.onboarding

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.onboarding.MihonApp
import mihon.core.platform.PlatformContext

/** Mihon runs only on Android, so desktop never finds it beside the app and the guide offers no move from it. */
@Inject
class NoMihonApp : MihonApp {
    override fun findInstalled(): String? = null

    override fun open(context: PlatformContext, id: String): Boolean = false
}
