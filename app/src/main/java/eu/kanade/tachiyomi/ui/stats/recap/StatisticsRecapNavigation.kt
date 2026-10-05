package eu.kanade.tachiyomi.ui.stats.recap

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import tachiyomi.core.common.Constants

object StatisticsRecapNavigation {
    const val ACTION_OPEN_RECAP = "app.katari.action.OPEN_STATISTICS_RECAP"
    private const val EXTRA_PERIOD = "statistics_recap_period"

    /** Opens the recap of [period] in the profile it was made for, dismissing notification [notificationId]. */
    fun pendingIntent(
        context: Context,
        profileId: Long,
        period: StatisticsRecapPeriod,
        notificationId: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_RECAP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.PROFILE_EXTRA, profileId)
            .putExtra(EXTRA_PERIOD, period)
            .putExtra("notificationId", notificationId)
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun periodOf(intent: Intent): StatisticsRecapPeriod? =
        IntentCompat.getSerializableExtra(intent, EXTRA_PERIOD, StatisticsRecapPeriod::class.java)
}
