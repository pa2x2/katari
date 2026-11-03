package eu.kanade.tachiyomi.data.statistics

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapNavigation
import eu.kanade.tachiyomi.ui.stats.recap.delivery.DueStatisticsRecap
import eu.kanade.tachiyomi.ui.stats.recap.delivery.notificationText
import eu.kanade.tachiyomi.util.system.notify

/** Posts a recap's notification, which opens the recap in its profile. */
class StatisticsRecapNotifier(private val context: Context) {
    fun post(recap: DueStatisticsRecap) {
        when (recap) {
            is DueStatisticsRecap.Month ->
                notify(Notifications.ID_STATISTICS_RECAP, Notifications.CHANNEL_MONTH_RECAP, recap)
            is DueStatisticsRecap.Year ->
                notify(Notifications.ID_STATISTICS_YEAR_RECAP, Notifications.CHANNEL_YEAR_RECAP, recap)
        }
    }

    private fun notify(id: Int, channelId: String, recap: DueStatisticsRecap) {
        val text = recap.notificationText(context)
        context.notify(id, channelId) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(text.title)
            setContentText(text.text)
            setStyle(NotificationCompat.BigTextStyle())
            setAutoCancel(true)
            setContentIntent(StatisticsRecapNavigation.pendingIntent(context, recap.profileId, recap.period, id))
        }
    }
}
