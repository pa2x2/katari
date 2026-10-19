package eu.kanade.tachiyomi.ui.library.update.report

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import tachiyomi.core.common.Constants

object LibraryUpdateReportNavigation {
    const val ACTION_OPEN_REPORT = "app.katari.action.OPEN_LIBRARY_UPDATE_REPORT"

    /** Opens the report in the profile whose update produced it. */
    fun pendingIntent(context: Context, profileId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_REPORT)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.PROFILE_EXTRA, profileId)
        return PendingIntent.getActivity(
            context,
            Notifications.idLibraryError(profileId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
