package eu.kanade.tachiyomi.ui.video.player.screen

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEndScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.ceil

private const val NEXT_EPISODE_PRELOAD_BEFORE_END_MS = 5_000L
private const val NEXT_EPISODE_COUNTDOWN_DURATION_MS = 8_000L
private const val NEXT_EPISODE_COUNTDOWN_TICK_MS = 33L

/**
 * The way on to the next episode: it is preloaded near the end, the end screen offers it with an auto-play countdown,
 * and picture-in-picture, which has no end screen, plays it straight away when auto-play is on. [advanceInFlight] is
 * set once an episode change has been asked for, so neither path asks twice, and cleared when the change fails or the
 * next playback starts.
 */
@Stable
internal class VideoPlayerNextEpisodeState(
    countdownElapsedMs: MutableState<Long>,
    endScreenDismissed: MutableState<Boolean>,
    advanceInFlight: MutableState<Boolean>,
) {
    // Time the end screen's countdown has run; it only runs while nothing else is open over the video.
    var countdownElapsedMs by countdownElapsedMs

    // Cancel or Close on the end screen, or asking for another episode once this one ended, hides the screen until the
    // episode ends again. An episode that fails to open leaves this one on screen, still ended, and must not bring back
    // a countdown that already ran out.
    var endScreenDismissed by endScreenDismissed
    var advanceInFlight by advanceInFlight

    /** Asking for another episode deals with this one's end, as Cancel or Close on the end screen does. */
    fun leaveEpisodeEnd(playbackEnded: Boolean) {
        if (playbackEnded) endScreenDismissed = true
    }

    @Composable
    fun Effects(
        ui: VideoPlayerWatchUiState,
        current: VideoPlayerViewModel.State.Ready,
        viewModel: VideoPlayerViewModel,
        autoPlayNext: Boolean,
        endScreenVisible: Boolean,
        isInPictureInPictureMode: Boolean,
        onNextEpisode: () -> Unit,
    ) {
        val nextEpisodeAvailable = current.nextChapterId != null
        val playbackRemainingMs = (ui.playbackSnapshot.durationMs - ui.playbackSnapshot.positionMs).coerceAtLeast(0L)
        val nearEpisodeEnd = ui.playbackSnapshot.durationMs > 0L &&
            playbackRemainingMs <= NEXT_EPISODE_PRELOAD_BEFORE_END_MS
        val countdownRunning = endScreenVisible &&
            autoPlayNext &&
            !ui.settingsVisible &&
            !ui.episodesDrawerVisible
        LaunchedEffect(nearEpisodeEnd, nextEpisodeAvailable) {
            if (nearEpisodeEnd && nextEpisodeAvailable) viewModel.preloadNextEpisode()
        }
        LaunchedEffect(ui.playbackSnapshot.playbackEnded) {
            if (!ui.playbackSnapshot.playbackEnded) {
                countdownElapsedMs = 0L
                endScreenDismissed = false
            }
        }
        LaunchedEffect(countdownRunning) {
            if (!countdownRunning) return@LaunchedEffect
            var lastTickAtMs = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(NEXT_EPISODE_COUNTDOWN_TICK_MS)
                val nowMs = SystemClock.elapsedRealtime()
                countdownElapsedMs += nowMs - lastTickAtMs
                lastTickAtMs = nowMs
                if (countdownElapsedMs >= NEXT_EPISODE_COUNTDOWN_DURATION_MS) {
                    onNextEpisode()
                    break
                }
            }
        }
        LaunchedEffect(
            ui.playbackSnapshot.playbackEnded,
            current.nextChapterId,
            isInPictureInPictureMode,
            ui.subtitleEditorVisible,
        ) {
            if (
                ui.playbackSnapshot.playbackEnded &&
                current.nextChapterId != null &&
                autoPlayNext &&
                isInPictureInPictureMode &&
                !ui.subtitleEditorVisible &&
                !endScreenDismissed
            ) {
                onNextEpisode()
            }
        }
    }

    @Composable
    fun EndScreen(
        visible: Boolean,
        nextEpisodeName: String,
        autoPlayNext: Boolean,
        onPlayNext: () -> Unit,
    ) {
        VideoPlayerEndScreen(
            visible = visible,
            nextEpisodeName = nextEpisodeName,
            countdown = if (autoPlayNext) {
                countdownElapsedMs / NEXT_EPISODE_COUNTDOWN_DURATION_MS.toFloat()
            } else {
                null
            },
            secondsLeft = if (autoPlayNext) {
                ceil(
                    (NEXT_EPISODE_COUNTDOWN_DURATION_MS - countdownElapsedMs)
                        .coerceAtLeast(0L) / 1_000.0,
                ).toInt()
            } else {
                null
            },
            onPlayNext = onPlayNext,
            onDismiss = { endScreenDismissed = true },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** The countdown and dismissal start over with each episode; an episode change in flight carries across them. */
@Composable
internal fun rememberVideoPlayerNextEpisodeState(chapterId: Long): VideoPlayerNextEpisodeState {
    val countdownElapsedMs = remember(chapterId) { mutableStateOf(0L) }
    val endScreenDismissed = remember(chapterId) { mutableStateOf(false) }
    val advanceInFlight = remember { mutableStateOf(false) }
    return remember(countdownElapsedMs, endScreenDismissed, advanceInFlight) {
        VideoPlayerNextEpisodeState(countdownElapsedMs, endScreenDismissed, advanceInFlight)
    }
}
