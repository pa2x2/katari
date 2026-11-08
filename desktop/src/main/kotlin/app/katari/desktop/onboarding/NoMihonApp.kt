package app.katari.desktop.onboarding

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.onboarding.MihonApp

/** Mihon runs only on Android, so desktop never finds it beside the app and the guide offers no move from it. */
@Inject
class NoMihonApp : MihonApp {
    override fun findInstalled(): String? = null

    override fun open(context: PlatformContext, id: String): Boolean = false
}
