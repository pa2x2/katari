package eu.kanade.tachiyomi.ui.video.player.mediacontrols

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mihon.entry.interactions.player.AnimePlayerMediaArtwork
import mihon.entry.interactions.player.AnimePlayerMediaCommands
import mihon.entry.interactions.player.AnimePlayerMediaControls
import mihon.entry.interactions.player.AnimePlayerMediaSession
import mihon.entry.interactions.player.AnimePlayerMediaState
import mihon.entry.interactions.player.AnimePlayerNavigation
import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.player.VideoPlayerState

/**
 * The desktop's [VideoPlayerMediaControls]: the player in the desktop's own media controls ([AnimePlayerMediaControls])
 * from the first episode's [attach] until [release], following each episode's player. What the controls ask for
 * arrives on their thread and is carried out on [scope], the UI thread the player is confined to. Stop ends the
 * player, as closing it does, and Raise brings it forward to fill the main window.
 */
internal class DesktopMediaControls(
    private val controls: AnimePlayerMediaControls,
    private val scope: CoroutineScope,
    private val navigation: () -> AnimePlayerNavigation,
) : VideoPlayerMediaControls {
    override var onNextEpisode: () -> Unit = {}
    override var onPreviousEpisode: () -> Unit = {}

    private var session: AnimePlayerMediaSession? = null
    private var player: VideoPlayer? = null
    private var following: Job? = null
    private var episodes = 0
    private val episodeNavigation = MutableStateFlow(EpisodeNavigation(canPlayPrevious = false, canPlayNext = false))

    override fun attach(
        player: VideoPlayer,
        metadata: VideoMetadata,
        seekStepSeconds: Int,
    ) {
        val session = session ?: controls.show(Commands()).also { session = it }
        this.player = player
        following?.cancel()
        val episode = ++episodes
        val artwork = AnimePlayerMediaArtwork(metadata.episodePreviewUrl, metadata.cover)
        following = scope.launch {
            combine(player.state, episodeNavigation, ::Pair).collect { (state, episodes) ->
                session.update(
                    AnimePlayerMediaState(
                        episode = episode,
                        title = metadata.title,
                        series = metadata.series,
                        artwork = artwork,
                        durationMs = state.durationMs,
                        positionMs = player.currentPositionMs,
                        playing = state.meansToPlay,
                        speed = state.speed,
                        hasPreviousEpisode = episodes.canPlayPrevious,
                        hasNextEpisode = episodes.canPlayNext,
                    ),
                )
            }
        }
    }

    override fun setEpisodeNavigation(canPlayPrevious: Boolean, canPlayNext: Boolean) {
        episodeNavigation.value = EpisodeNavigation(canPlayPrevious, canPlayNext)
    }

    fun release() {
        following?.cancel()
        following = null
        player = null
        session?.close()
        session = null
    }

    private inner class Commands : AnimePlayerMediaCommands {
        override fun play() = onPlayer(::play)

        override fun pause() = onPlayer(VideoPlayer::pause)

        override fun togglePlayback() = onPlayer { player ->
            if (player.state.value.playWhenReady) player.pause() else play(player)
        }

        override fun stop() = onUiThread { navigation().close() }

        override fun seekTo(positionMs: Long) = onPlayer { it.seekTo(positionMs) }

        override fun previousEpisode() = onUiThread { onPreviousEpisode() }

        override fun nextEpisode() = onUiThread { onNextEpisode() }

        override fun raise() = onUiThread { navigation().bringToMainWindow() }
    }

    // As the player's own Play does, an episode that ended plays again from the start
    private fun play(player: VideoPlayer) {
        if (player.state.value.playback == VideoPlayerState.Playback.Ended) player.seekTo(0L)
        player.play()
    }

    private fun onPlayer(action: (VideoPlayer) -> Unit) = onUiThread { player?.let(action) }

    private fun onUiThread(action: () -> Unit) {
        scope.launch { action() }
    }
}

private data class EpisodeNavigation(val canPlayPrevious: Boolean, val canPlayNext: Boolean)

// Buffering counts, as shells would otherwise show a pause at every stall
private val VideoPlayerState.meansToPlay: Boolean
    get() = playWhenReady && when (playback) {
        VideoPlayerState.Playback.Ready, VideoPlayerState.Playback.Buffering -> true
        VideoPlayerState.Playback.Idle, VideoPlayerState.Playback.Ended -> false
    }
