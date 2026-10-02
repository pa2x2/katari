package eu.kanade.tachiyomi.ui.library.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import tachiyomi.core.common.Constants

object LibraryUpdateSettingsNavigation {
    const val ACTION_OPEN_SETTINGS = "app.katari.action.OPEN_LIBRARY_UPDATE_SETTINGS"

    /** Opens the Library updates settings of the profile a notification is about, dismissing the notification. */
    fun pendingIntent(context: Context, profileId: Long, notificationId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.PROFILE_EXTRA, profileId)
            .putExtra("notificationId", notificationId)
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
