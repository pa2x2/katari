package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.markers.MarkerAutoSkip
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipCountdown
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipOffer
import eu.kanade.tachiyomi.ui.video.player.markers.skipOfferAt
import kotlinx.coroutines.delay
import mihon.entry.interactions.settings.AnimeMarkerSkipMode
import mihon.entry.viewer.settings.ViewerSettingBinding

/** An automatic skip, kept for Undo: [fromMs] is where playback was when it skipped. */
internal data class VideoPlayerSkippedMarker(val marker: PlaybackMarker, val fromMs: Long)

/**
 * The skip button's offer and the skips that happen by themselves in one episode's visit, under the title's skip mode:
 * at once, or once the button's countdown fills while the episode plays. A null mode is one still loading, under which
 * nothing is offered or skipped.
 */
@Stable
internal class VideoPlayerMarkerSkip {
    private val autoSkip = MarkerAutoSkip()
    private var countdownMarker: PlaybackMarker? = null
    private val countdownElapsedMs = mutableLongStateOf(0L)

    var skipped by mutableStateOf<VideoPlayerSkippedMarker?>(null)
        private set

    fun offerAt(
        mode: AnimeMarkerSkipMode?,
        positionMs: Long,
        durationMs: Long,
        markers: List<PlaybackMarker>,
        hasNextEpisode: Boolean,
    ): VideoPlayerSkipOffer? {
        if (mode == null || mode == AnimeMarkerSkipMode.OFF) return null
        return skipOfferAt(positionMs, durationMs, markers, hasNextEpisode)
    }

    /** The countdown the button shows for [offer], null when the mode has none or this marker's ran or was stopped. */
    fun countdownFor(mode: AnimeMarkerSkipMode?, offer: VideoPlayerSkipOffer?): VideoPlayerSkipCountdown? {
        if (mode != AnimeMarkerSkipMode.COUNTDOWN || offer == null || !autoSkip.pending(offer)) return null
        val marker = offer.marker
        return VideoPlayerSkipCountdown(
            progress = {
                if (countdownMarker == marker) countdownElapsedMs.longValue / COUNTDOWN_MS.toFloat() else 0f
            },
            onDismiss = { autoSkip.decline(marker) },
        )
    }

    /**
     * Skips [offer] by itself under [mode] while this runs: at once when automatic, or after the countdown fills, which
     * picks up where it stopped when playback paused. A yes is kept for Undo.
     */
    suspend fun skipsByItself(
        mode: AnimeMarkerSkipMode?,
        offer: VideoPlayerSkipOffer,
        positionMs: () -> Long,
    ): Boolean {
        when (mode) {
            AnimeMarkerSkipMode.AUTOMATIC -> Unit
            AnimeMarkerSkipMode.COUNTDOWN -> if (!countDown(offer)) return false
            else -> return false
        }
        if (!autoSkip.claim(offer)) return false
        skipped = VideoPlayerSkippedMarker(offer.marker, positionMs())
        return true
    }

    private suspend fun countDown(offer: VideoPlayerSkipOffer): Boolean {
        if (countdownMarker != offer.marker) {
            countdownMarker = offer.marker
            countdownElapsedMs.longValue = 0L
        }
        var lastFrameMs = withFrameMillis { it }
        while (countdownElapsedMs.longValue < COUNTDOWN_MS) {
            if (!autoSkip.pending(offer)) return false
            val frameMs = withFrameMillis { it }
            countdownElapsedMs.longValue += frameMs - lastFrameMs
            lastFrameMs = frameMs
        }
        return true
    }

    fun dismissSkipped() {
        skipped = null
    }
}

@Composable
internal fun rememberVideoPlayerMarkerSkip(chapterId: Long): VideoPlayerMarkerSkip =
    remember(chapterId) { VideoPlayerMarkerSkip() }

/** The title's skip mode setting, with its override for the title; null while it loads. */
@Composable
internal fun rememberMarkerSkipSetting(
    host: VideoPlayerScreenHost,
    entryId: Long,
): ViewerSettingBinding<AnimeMarkerSkipMode>? {
    val binding by produceState<ViewerSettingBinding<AnimeMarkerSkipMode>?>(null, entryId) {
        value = host.settings.markerSkip(entryId)
    }
    return binding
}

/**
 * Skips [offer] while the episode plays, when the mode skips by itself and this marker wasn't skipped yet. The Undo
 * notice for the skip clears itself after a while.
 */
@Composable
internal fun VideoPlayerMarkerSkip.Effects(
    mode: AnimeMarkerSkipMode?,
    offer: VideoPlayerSkipOffer?,
    ui: VideoPlayerWatchUiState,
    seekTo: (Long) -> Unit,
) {
    val playing = ui.playbackSnapshot.isPlaying && !ui.startupOverlayVisible && !ui.isScrubbing
    LaunchedEffect(offer?.marker, mode, playing) {
        if (offer == null || !playing) return@LaunchedEffect
        if (skipsByItself(mode, offer) { ui.playbackSnapshot.positionMs }) seekTo(offer.targetMs)
    }
    val shown = skipped
    if (shown != null) {
        LaunchedEffect(shown) {
            delay(SKIPPED_NOTICE_MS)
            dismissSkipped()
        }
    }
}

private const val SKIPPED_NOTICE_MS = 8_000L
private const val COUNTDOWN_MS = 5_000L
