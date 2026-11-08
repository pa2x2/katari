package eu.kanade.presentation.more.settings.screen.advanced

import android.content.Intent
import android.provider.Settings
import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject

@Inject
class AndroidAppNotifications : AppNotifications {
    override val isOffered: Boolean = true

    override val hasSystemSettings: Boolean = true

    override fun openSystemSettings(context: PlatformContext) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
}
