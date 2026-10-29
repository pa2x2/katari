package eu.kanade.tachiyomi.data.statistics

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.presentation.more.stats.components.statisticsDurationFormatter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapNavigation
import eu.kanade.tachiyomi.ui.stats.recap.delivery.DueStatisticsRecap
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import tachiyomi.i18n.context.stringResource
import java.time.format.TextStyle
import java.util.Locale

/** Posts a recap's notification, which opens the recap in its profile. */
class StatisticsRecapNotifier(private val context: Context) {
    fun post(recap: DueStatisticsRecap) {
        when (recap) {
            is DueStatisticsRecap.Month -> postMonth(recap)
            is DueStatisticsRecap.Year -> postYear(recap)
        }
    }

    private fun postMonth(recap: DueStatisticsRecap.Month) {
        val monthName = recap.period.month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        notify(
            id = Notifications.ID_STATISTICS_RECAP,
            channelId = Notifications.CHANNEL_MONTH_RECAP,
            recap = recap,
            title = context.stringResource(
                MR.strings.statistics_recap_notification_title,
                monthName,
                context.statisticsDurationFormatter()(recap.headline.durationMillis),
            ),
            text = context.stringResource(
                MR.strings.statistics_recap_notification_text,
                context.pluralStringResource(
                    MR.plurals.statistics_active_day_count,
                    recap.headline.activeDays,
                    recap.headline.activeDays,
                ),
            ),
        )
    }

    private fun postYear(recap: DueStatisticsRecap.Year) {
        val period = recap.period
        val label = if (period.isSoFar) {
            context.stringResource(MR.strings.statistics_recap_year_so_far, period.year.toString())
        } else {
            period.year.toString()
        }
        notify(
            id = Notifications.ID_STATISTICS_YEAR_RECAP,
            channelId = Notifications.CHANNEL_YEAR_RECAP,
            recap = recap,
            title = context.stringResource(MR.strings.statistics_year_recap_notification_title, label),
            text = context.stringResource(
                MR.strings.statistics_year_recap_notification_text,
                context.statisticsDurationFormatter()(recap.headline.durationMillis),
            ),
        )
    }

    private fun notify(id: Int, channelId: String, recap: DueStatisticsRecap, title: String, text: String) {
        context.notify(id, channelId) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(title)
            setContentText(text)
            setStyle(NotificationCompat.BigTextStyle())
            setAutoCancel(true)
            setContentIntent(StatisticsRecapNavigation.pendingIntent(context, recap.profileId, recap.period, id))
        }
    }
}
