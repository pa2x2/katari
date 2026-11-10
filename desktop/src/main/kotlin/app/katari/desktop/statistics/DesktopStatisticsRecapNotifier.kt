package app.katari.desktop.statistics

import app.katari.core.platform.PlatformContext
import app.katari.desktop.home.HomeDestination
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationResponse
import app.katari.notification.AppNotificationKind
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.stats.recap.delivery.DueStatisticsRecap
import eu.kanade.tachiyomi.ui.stats.recap.delivery.notificationText

/** Posts a recap's notification, which opens the recap in its profile, one for each kind as on Android. */
@Inject
class DesktopStatisticsRecapNotifier(
    private val context: PlatformContext,
    private val notifications: DesktopNotifications,
) {
    fun post(recap: DueStatisticsRecap) {
        val text = recap.notificationText(context)
        val (key, kind) = when (recap) {
            is DueStatisticsRecap.Month -> MONTH_RECAP to AppNotificationKind.MONTH_RECAP
            is DueStatisticsRecap.Year -> YEAR_RECAP to AppNotificationKind.YEAR_RECAP
        }
        notifications.show(
            key,
            DesktopNotification(
                kind = kind,
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
