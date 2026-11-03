package eu.kanade.tachiyomi.ui.more

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.onboarding.MihonApp
import mihon.core.platform.PlatformContext

/** Mihon's release, FOSS and debug builds, by package name. */
@Inject
class AndroidMihonApp(private val application: Application) : MihonApp {
    override fun findInstalled(): String? = application.findInstalledMihonPackage()

    override fun open(context: PlatformContext, id: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(id) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
