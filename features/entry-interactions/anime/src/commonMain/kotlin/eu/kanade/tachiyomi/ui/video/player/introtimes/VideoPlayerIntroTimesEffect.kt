package eu.kanade.tachiyomi.ui.video.player.introtimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.markers.skippable
import kotlinx.coroutines.flow.first

/** Whether the playing episode's times are looked up online: the setting is on and its source marks nothing to skip. */
internal fun asksIntroTimesOnline(settingOn: Boolean, streamMarkers: List<PlaybackMarker>): Boolean =
    settingOn && streamMarkers.none { it.skippable }

/** The markers the player goes by: the source's, with the times found online for [chapterId] when it asks for them. */
internal fun playbackMarkers(
    streamMarkers: List<PlaybackMarker>,
    chapterId: Long,
    asksOnline: Boolean,
    introTimes: VideoPlayerIntroTimesState?,
): List<PlaybackMarker> {
    val online = introTimes?.takeIf { asksOnline && it.chapterId == chapterId }?.markers.orEmpty()
    return if (online.isEmpty()) streamMarkers else streamMarkers + online
}

/**
 * Asks for [chapterId]'s times once [lengthMs] reports its length, which AniSkip needs. Placed after the effect that
 * resets the playback snapshot for a new episode, so the length it waits for is never the previous episode's.
 */
@Composable
internal fun VideoPlayerIntroTimes.Effect(chapterId: Long, asksOnline: Boolean, lengthMs: () -> Long) {
    LaunchedEffect(this, chapterId, asksOnline) {
        if (!asksOnline) {
            clear()
            return@LaunchedEffect
        }
        request(chapterId, snapshotFlow(lengthMs).first { it > 0L })
    }
}
