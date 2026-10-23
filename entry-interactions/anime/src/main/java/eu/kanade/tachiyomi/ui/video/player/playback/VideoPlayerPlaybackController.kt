package eu.kanade.tachiyomi.ui.video.player.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import eu.kanade.tachiyomi.ui.video.player.VideoActivePlaybackClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.IdentityHashMap

/**
 * Owns the player the viewer watches, one per episode: [attach] replaces the previous episode's player and saves the
 * new one's progress every 10 seconds from then on, [flush] saves it now, and [release] lets it go. Saved progress
 * carries the active playback time since the last save, measured by the clock [track] registered for that player.
 */
internal class VideoPlayerPlaybackController(
    private val scope: CoroutineScope,
    private val onPersist: (positionMs: Long, durationMs: Long, activeDurationMs: Long) -> Unit,
) {
    var player by mutableStateOf<Player?>(null)
        private set
    private var progressSaveJob: Job? = null
    private val playbackClocks = IdentityHashMap<Player, VideoActivePlaybackClock>()

    fun track(player: Player, clock: VideoActivePlaybackClock) {
        playbackClocks[player] = clock
    }

    /** Releases the previous episode's player without saving it: the episode change already flushed it. */
    fun attach(player: Player) {
        release(persistState = false)
        this.player = player
        startProgressSaves(player)
    }

    private fun startProgressSaves(player: Player) {
        progressSaveJob?.cancel()
        progressSaveJob = scope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                persist(player)
            }
        }
    }

    fun flush() {
        player?.let(::persist)
    }

    fun persist(player: Player) {
        onPersist(
            player.currentPosition,
            player.duration.takeIf { it > 0L } ?: 0L,
            playbackClocks[player]?.checkpoint() ?: 0L,
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
