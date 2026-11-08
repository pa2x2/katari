package eu.kanade.tachiyomi.ui.stats.recap.delivery

import app.katari.core.platform.PlatformContext
import eu.kanade.presentation.more.stats.components.statisticsDurationFormatter
import tachiyomi.i18n.MR
import tachiyomi.i18n.platform.pluralStringResource
import tachiyomi.i18n.platform.stringResource
import tachiyomi.i18n.statistics_active_day_count
import tachiyomi.i18n.statistics_recap_notification_text
import tachiyomi.i18n.statistics_recap_notification_title
import tachiyomi.i18n.statistics_recap_year_so_far
import tachiyomi.i18n.statistics_year_recap_notification_text
import tachiyomi.i18n.statistics_year_recap_notification_title
import java.time.format.TextStyle
import java.util.Locale

/** What the notification of a due recap says, worded the same on every platform. */
class StatisticsRecapNotificationText(val title: String, val text: String)

fun DueStatisticsRecap.notificationText(context: PlatformContext): StatisticsRecapNotificationText {
    val duration = context.statisticsDurationFormatter()(headline.durationMillis)
    return when (this) {
        is DueStatisticsRecap.Month -> {
            val monthName = period.month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            val activeDays = context.pluralStringResource(
                MR.plurals.statistics_active_day_count,
                headline.activeDays,
                headline.activeDays,
            )
            StatisticsRecapNotificationText(
                title = context.stringResource(MR.strings.statistics_recap_notification_title, monthName, duration),
                text = context.stringResource(MR.strings.statistics_recap_notification_text, activeDays),
            )
        }
        is DueStatisticsRecap.Year -> {
            val label = if (period.isSoFar) {
                context.stringResource(MR.strings.statistics_recap_year_so_far, period.year.toString())
            } else {
                period.year.toString()
            }
            StatisticsRecapNotificationText(
                title = context.stringResource(MR.strings.statistics_year_recap_notification_title, label),
                text = context.stringResource(MR.strings.statistics_year_recap_notification_text, duration),
            )
        }
    }
}
