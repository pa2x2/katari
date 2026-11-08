package app.katari.sync.server.web.page

import kotlin.time.Duration.Companion.milliseconds

/** "2 minutes ago", "yesterday", "40 days ago": when a device last synced, as the account lists show it. */
internal fun lastSyncText(lastSeenMillis: Long?, nowMillis: Long): String {
    lastSeenMillis ?: return "never"
    val ago = (nowMillis - lastSeenMillis).coerceAtLeast(0).milliseconds
    return when {
        ago.inWholeMinutes < 1 -> "just now"
        ago.inWholeHours < 1 -> plural(ago.inWholeMinutes, "minute") + " ago"
        ago.inWholeDays < 1 -> plural(ago.inWholeHours, "hour") + " ago"
        ago.inWholeDays < 2 -> "yesterday"
        else -> plural(ago.inWholeDays, "day") + " ago"
    }
}

internal fun plural(count: Long, unit: String): String = if (count == 1L) "1 $unit" else "$count ${unit}s"
