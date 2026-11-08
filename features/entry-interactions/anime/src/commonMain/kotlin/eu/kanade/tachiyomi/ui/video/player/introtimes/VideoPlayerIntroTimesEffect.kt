package eu.kanade.tachiyomi.ui.video.player.introtimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.entry.interactions.anime.introtimes.OnlineMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.markers.skippable
import kotlinx.coroutines.flow.first

/**
 * The services the playing episode's times are looked up with: those turned on, while the setting is and its source
 * marks nothing to skip. Empty when none is asked.
 */
internal fun introTimesServices(
    settingOn: Boolean,
    turnedOn: Set<IntroTimesService>,
    streamMarkers: List<PlaybackMarker>,
): Set<IntroTimesService> = if (settingOn && streamMarkers.none { it.skippable }) turnedOn else emptySet()

/** The markers found online for [chapterId], when it asks for them. */
internal fun onlineMarkers(
    chapterId: Long,
    services: Set<IntroTimesService>,
    introTimes: VideoPlayerIntroTimesState?,
): List<OnlineMarker> =
    introTimes?.takeIf { services.isNotEmpty() && it.chapterId == chapterId }?.markers.orEmpty()

/**
 * Asks for [chapterId]'s times once [lengthMs] reports its length, which both services need. Placed after the effect
 * that resets the playback snapshot for a new episode, so the length it waits for is never the previous episode's.
 */
@Composable
internal fun VideoPlayerIntroTimes.Effect(chapterId: Long, services: Set<IntroTimesService>, lengthMs: () -> Long) {
    LaunchedEffect(this, chapterId, services) {
        if (services.isEmpty()) {
            clear()
            return@LaunchedEffect
        }
        request(chapterId, snapshotFlow(lengthMs).first { it > 0L }, services)
    }
}
