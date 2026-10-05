package eu.kanade.tachiyomi.ui.stats.recap

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import tachiyomi.core.common.Constants

object StatisticsRecapNavigation {
    const val ACTION_OPEN_RECAP = "app.katari.action.OPEN_STATISTICS_RECAP"
    const val EXTRA_START_DATE = "statistics_recap_start"
    const val EXTRA_END_DATE = "statistics_recap_end"
    const val EXTRA_PERIOD_LABEL = "statistics_recap_period"

    /** Opens the recap of one period in the profile it was made for, dismissing the recap notification. */
    fun pendingIntent(
        context: Context,
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
        periodLabel: String,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_RECAP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.PROFILE_EXTRA, profileId)
            .putExtra(EXTRA_START_DATE, startLocalDate)
            .putExtra(EXTRA_END_DATE, endLocalDate)
            .putExtra(EXTRA_PERIOD_LABEL, periodLabel)
            .putExtra("notificationId", Notifications.ID_STATISTICS_RECAP)
        return PendingIntent.getActivity(
            context,
            Notifications.ID_STATISTICS_RECAP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
