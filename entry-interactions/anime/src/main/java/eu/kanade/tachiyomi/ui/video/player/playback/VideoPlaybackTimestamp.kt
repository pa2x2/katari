package eu.kanade.tachiyomi.ui.video.player.playback

import java.util.Locale

internal fun formatPlaybackTimestamp(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L)
    val seconds = totalSeconds % 60L
    val totalMinutes = totalSeconds / 60L
    val minutes = totalMinutes % 60L
    val hours = totalMinutes / 60L

    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", totalMinutes, seconds)
    }
}
