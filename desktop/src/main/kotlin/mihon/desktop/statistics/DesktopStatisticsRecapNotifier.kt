package mihon.desktop.statistics

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.stats.recap.delivery.DueStatisticsRecap
import eu.kanade.tachiyomi.ui.stats.recap.delivery.notificationText
import mihon.core.platform.PlatformContext
import mihon.desktop.home.HomeDestination
import mihon.desktop.notification.DesktopNotification
import mihon.desktop.notification.DesktopNotifications
import mihon.desktop.notification.NotificationResponse

/** Posts a recap's notification, which opens the recap in its profile, one for each kind as on Android. */
@Inject
class DesktopStatisticsRecapNotifier(
    private val context: PlatformContext,
    private val notifications: DesktopNotifications,
) {
    fun post(recap: DueStatisticsRecap) {
        val text = recap.notificationText(context)
        val key = when (recap) {
            is DueStatisticsRecap.Month -> MONTH_RECAP
            is DueStatisticsRecap.Year -> YEAR_RECAP
        }
        notifications.show(
            key,
            DesktopNotification(
                title = text.title,
                body = text.text,
                onClick = NotificationResponse.Open(HomeDestination.StatisticsRecap(recap.profileId, recap.period)),
            ),
        )
    }

    private companion object {
        const val MONTH_RECAP = "statistics-month-recap"
        const val YEAR_RECAP = "statistics-year-recap"
    }
}
