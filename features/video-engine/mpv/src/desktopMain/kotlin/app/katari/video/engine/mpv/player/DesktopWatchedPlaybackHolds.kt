package app.katari.video.engine.mpv.player

import app.katari.video.engine.mpv.audio.AudioOutputWatch
import app.katari.video.engine.spi.player.VideoPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * What the watched player holds on the desktop: a pause when the sound's output goes away while it means to play, as
 * Android's becoming-noisy pause does. Nothing else plays over it, so it never waits for anything, and keeping the
 * screen awake belongs to the window that shows it.
 */
internal class DesktopWatchedPlaybackHolds(private val listener: WatchedPlaybackHolds.Listener) : WatchedPlaybackHolds {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Read on libpulse's thread, set on the UI thread.
    @Volatile
    private var meansToPlay = false

    private val outputWatch = AudioOutputWatch {
        if (meansToPlay) scope.launch { if (meansToPlay) listener.onPauseRequested() }
    }.also(AudioOutputWatch::start)

    override val allowsPlaying: Boolean = true

    override val volumeMultiplier: Float = 1f

    override fun setPlayWhenReady(playWhenReady: Boolean, playback: VideoPlayerState.Playback) {
        update(playWhenReady, playback)
    }

    override fun update(playWhenReady: Boolean, playback: VideoPlayerState.Playback) {
        meansToPlay = playWhenReady
    }

    override fun release() {
        meansToPlay = false
        scope.cancel()
        outputWatch.release()
    }
}
