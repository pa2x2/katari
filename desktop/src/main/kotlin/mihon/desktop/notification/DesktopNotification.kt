package mihon.desktop.notification

import mihon.desktop.home.HomeDestination
import kotlin.time.Duration

/**
 * A notification the app shows on the desktop where Android posts one in the shade. One with [progress] tells of work
 * under way, as Android's ongoing notifications do; the others tell a result.
 *
 * @param body the lines under the title, as plain text; null for none.
 * @param onClick what clicking the notification does; null where Android's notification opens nothing.
 * @param timeout how long it shows before the desktop takes it away; null for the desktop's own default.
 */
class DesktopNotification(
    val title: String,
    val body: String? = null,
    val progress: NotificationProgress? = null,
    val urgency: NotificationUrgency = NotificationUrgency.Normal,
    val onClick: NotificationResponse? = null,
    val actions: List<NotificationAction> = emptyList(),
    val timeout: Duration? = null,
) {
    val isOngoing: Boolean
        get() = progress != null
}

/** How far the work got: [done] of [total], or of an unknown amount while [total] is 0. */
data class NotificationProgress(val done: Int, val total: Int)

enum class NotificationUrgency {
    /** Shown quietly, as Android's low-importance channels show progress: listed, without popping up if avoidable. */
    Low,
    Normal,
}

class NotificationAction(val label: String, val response: NotificationResponse)

sealed interface NotificationResponse {
    /** Brings the app's window forward and opens [destination] in it. */
    data class Open(val destination: HomeDestination) : NotificationResponse

    /** Runs [action] and leaves the window as it is, such as for stopping the work a notification shows. */
    class Run(val action: suspend () -> Unit) : NotificationResponse
}
