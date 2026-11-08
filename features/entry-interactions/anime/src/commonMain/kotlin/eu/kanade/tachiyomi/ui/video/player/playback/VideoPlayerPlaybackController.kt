package eu.kanade.tachiyomi.ui.video.player.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoActivePlaybackClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.IdentityHashMap

/**
 * Owns the player the viewer watches, one per episode: [attach] replaces the previous episode's player and saves the
 * new one's progress every 10 seconds from then on, [flush] saves it now, as the player pauses or is left, and
 * [release] lets it go; a player showing an error saves nothing. Saves from [flush] and [release] are settled: where
 * the person put the episode down. Saved progress carries the active playback time since the last save, measured by the clock
 * [track] registered for that player.
 */
internal class VideoPlayerPlaybackController(
    private val scope: CoroutineScope,
    private val onPersist: (positionMs: Long, durationMs: Long, activeDurationMs: Long, settled: Boolean) -> Unit,
) {
    var player by mutableStateOf<VideoPlayer?>(null)
        private set
    private var progressSaveJob: Job? = null
    private val playbackClocks = IdentityHashMap<VideoPlayer, VideoActivePlaybackClock>()

    fun track(player: VideoPlayer, clock: VideoActivePlaybackClock) {
        playbackClocks[player] = clock
    }

    /** Releases the previous episode's player without saving it: the episode change already flushed it. */
    fun attach(player: VideoPlayer) {
        release(persistState = false)
        this.player = player
        startProgressSaves(player)
    }

    private fun startProgressSaves(player: VideoPlayer) {
        progressSaveJob?.cancel()
        progressSaveJob = scope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                persist(player, settled = false)
            }
        }
    }

    fun flush() {
        player?.let { persist(it, settled = true) }
    }

    /** Saves [player]'s progress while it goes on playing, as before switching its source. */
    fun persist(player: VideoPlayer) {
        persist(player, settled = false)
    }

    private fun persist(player: VideoPlayer, settled: Boolean) {
        // A player showing an error has nothing to add: what it played was saved within the last 10 seconds, and one
        // that failed to load knows no duration, so saving it would wipe the episode's.
        if (player.state.value.error != null) return
        onPersist(
            player.currentPositionMs,
            player.state.value.durationMs?.takeIf { it > 0L } ?: 0L,
            playbackClocks[player]?.checkpoint() ?: 0L,
            settled,
        )
    }

    fun release(persistState: Boolean = true) {
        progressSaveJob?.cancel()
        progressSaveJob = null
        if (persistState) {
            flush()
        }
        player?.let { currentPlayer ->
            playbackClocks.remove(currentPlayer)?.setActive(false)
            currentPlayer.release()
        }
        player = null
    }

    private companion object {
        const val PROGRESS_SAVE_INTERVAL_MS = 10_000L
    }
}
