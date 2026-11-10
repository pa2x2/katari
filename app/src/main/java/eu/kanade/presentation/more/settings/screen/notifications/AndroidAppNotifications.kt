package eu.kanade.presentation.more.settings.screen.notifications

import android.content.Intent
import android.provider.Settings
import dev.zacsweers.metro.Inject

@Inject
class AndroidAppNotifications : AppNotifications {
    override val isOffered: Boolean = true

    override val choices = NotificationChoices.InSystemSettings { context ->
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
}
