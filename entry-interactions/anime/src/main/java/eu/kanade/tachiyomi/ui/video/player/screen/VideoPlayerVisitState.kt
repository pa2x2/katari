package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mihon.entry.interactions.settings.AnimePlayerOrientation

/** Player screen state that lasts the whole visit to the player rather than one episode. */
@Stable
internal class VideoPlayerVisitState(
    gestureGuideVisible: Boolean,
    showsRemainingTime: Boolean,
) {
    var controlsLocked by mutableStateOf(false)

    // Set by the rotate button; it outlives episode changes and lasts until the player closes.
    var orientationOverride by mutableStateOf<AnimePlayerOrientation?>(null)

    // Shown once per profile, until any tap on it.
    var gestureGuideVisible by mutableStateOf(gestureGuideVisible)
    var showsRemainingTime by mutableStateOf(showsRemainingTime)

    // The last known shape, kept while the next episode loads so the screen doesn't turn back and forth.
    var videoAspectRatio by mutableStateOf<Float?>(null)

    // Applied once the player exists, as an intent can arrive before the stream has been resolved.
    var pendingPausedStartMs by mutableStateOf<Long?>(null)
}
