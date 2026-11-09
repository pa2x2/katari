package app.katari.entry.interactions.anime.media.immersive

import app.katari.entry.interactions.player.AnimePlayerMediaArtwork
import app.katari.entry.interactions.player.AnimePlayerMediaCommands
import app.katari.entry.interactions.player.AnimePlayerMediaControls
import app.katari.entry.interactions.player.AnimePlayerMediaSession
import app.katari.entry.interactions.player.AnimePlayerMediaState
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoMetadata
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.meansToPlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The feed's shown clip in the desktop's own media controls ([AnimePlayerMediaControls]): one session from the first
 * clip [show]n until the feed has shown none for a moment, which each shown clip takes over, so moving between clips
 * leaves the feed in the shell's media widget. Playing, pausing and seeking reach the clip. The feed's titles aren't
 * episodes of one series, so next and previous are never offered, and Stop pauses, as the feed stays open.
 */
internal class ClipMediaControls(private val controls: AnimePlayerMediaControls) {
    // The UI thread, which players are confined to and which the controls' requests are carried out on
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var session: AnimePlayerMediaSession? = null
    private var shown: Clip? = null
    private var following: Job? = null
    private var closing: Job? = null
    private var clips = 0

    /** Puts [player] in the controls until the returned handle closes. Called on the UI thread. */
    fun show(player: VideoPlayer, metadata: VideoMetadata, target: AnimeClipInputTarget): AutoCloseable {
        closing?.cancel()
        closing = null
        val session = session ?: controls.show(Commands()).also { session = it }
        val clip = Clip(player, target)
        shown = clip
        val number = ++clips
        val artwork = AnimePlayerMediaArtwork(metadata.episodePreviewUrl, metadata.cover)
        following?.cancel()
        following = scope.launch {
            player.state.collect { state ->
                session.update(
                    AnimePlayerMediaState(
                        episode = number,
                        title = metadata.title,
                        series = metadata.series,
                        artwork = artwork,
                        durationMs = state.durationMs,
                        positionMs = player.currentPositionMs,
                        playing = state.meansToPlay,
                        speed = state.speed,
                        hasPreviousEpisode = false,
                        hasNextEpisode = false,
                    ),
                )
            }
        }
        return AutoCloseable { hide(clip) }
    }

    private fun hide(clip: Clip) {
        if (shown !== clip) return
        shown = null
        following?.cancel()
        following = null
        closing = scope.launch {
            delay(CLOSE_GRACE_MS)
            session?.close()
            session = null
        }
    }

    private inner class Commands : AnimePlayerMediaCommands {
        override fun play() = onClip { it.target.setPlaying(true) }

        override fun pause() = onClip { it.target.setPlaying(false) }

        override fun togglePlayback() = onClip { it.target.togglePlayback() }

        override fun stop() = pause()

        override fun seekTo(positionMs: Long) = onClip { it.player.seekTo(positionMs) }

        override fun previousEpisode() = Unit

        override fun nextEpisode() = Unit

        // The feed is in the main window already
        override fun raise() = Unit
    }

    private fun onClip(action: (Clip) -> Unit) {
        scope.launch { shown?.let(action) }
    }

    private class Clip(val player: VideoPlayer, val target: AnimeClipInputTarget)
}

// Long enough for the next clip to be shown after the last one, short enough that leaving the feed clears the widget
private const val CLOSE_GRACE_MS = 1_000L
