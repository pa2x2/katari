package mihon.desktop.player.mediacontrols

import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.mpris.MprisCommands
import mihon.desktop.mpris.MprisPlayer
import mihon.desktop.mpris.MprisPlayerState
import mihon.entry.interactions.player.AnimePlayerMediaArtwork
import mihon.entry.interactions.player.AnimePlayerMediaCommands
import mihon.entry.interactions.player.AnimePlayerMediaControls
import mihon.entry.interactions.player.AnimePlayerMediaSession
import mihon.entry.interactions.player.AnimePlayerMediaState

/**
 * Linux: the player's media controls as an MPRIS player ([MprisPlayer]) named after the app's build, so a release and
 * a development build open together each show as their own. Each session's artwork is a file of its own
 * ([PlayerArtworkFiles]), filled in once it has loaded.
 */
internal class MprisAnimePlayerMediaControls(
    private val artworkFiles: PlayerArtworkFiles,
) : AnimePlayerMediaControls {
    override fun show(commands: AnimePlayerMediaCommands): AnimePlayerMediaSession = Session(commands)

    private inner class Session(commands: AnimePlayerMediaCommands) : AnimePlayerMediaSession {
        private val player = MprisPlayer(
            busName = BUS_NAME_PREFIX + DesktopAppVariant.current.directoryName.replace('-', '_'),
            identity = DesktopAppVariant.current.displayName,
            desktopEntry = DesktopAppVariant.current.applicationId,
            commands = ShellCommands(commands),
        )
        private val lock = Any()
        private var latest: AnimePlayerMediaState? = null
        private var artwork: Artwork? = null

        override fun update(state: AnimePlayerMediaState) = synchronized(lock) {
            latest = state
            if (artwork?.let { it.episode == state.episode && it.source == state.artwork } != true) {
                artwork?.loading?.close()
                val episode = state.episode
                artwork = Artwork(
                    episode = episode,
                    source = state.artwork,
                    loading = artworkFiles.write(state.artwork) { uri -> onArtwork(episode, uri) },
                )
            }
            publish()
        }

        override fun close() {
            synchronized(lock) {
                artwork?.loading?.close()
                artwork = null
            }
            player.close()
        }

        private fun onArtwork(episode: Int, uri: String) = synchronized(lock) {
            val artwork = artwork?.takeIf { it.episode == episode } ?: return@synchronized
            artwork.uri = uri
            publish()
        }

        private fun publish() {
            val state = latest ?: return
            player.update(
                MprisPlayerState(
                    trackId = "$TRACK_PATH/${state.episode}",
                    title = state.title,
                    series = state.series,
                    artUrl = artwork?.uri,
                    lengthUs = state.durationMs?.times(US_PER_MS),
                    positionUs = state.positionMs * US_PER_MS,
                    playing = state.playing,
                    rate = state.speed.toDouble(),
                    canGoPrevious = state.hasPreviousEpisode,
                    canGoNext = state.hasNextEpisode,
                ),
            )
        }
    }

    private class Artwork(val episode: Int, val source: AnimePlayerMediaArtwork, val loading: AutoCloseable) {
        var uri: String? = null
    }

    private class ShellCommands(private val commands: AnimePlayerMediaCommands) : MprisCommands {
        override fun play() = commands.play()

        override fun pause() = commands.pause()

        override fun playPause() = commands.togglePlayback()

        override fun stop() = commands.stop()

        override fun setPosition(positionUs: Long) = commands.seekTo(positionUs / US_PER_MS)

        override fun previous() = commands.previousEpisode()

        override fun next() = commands.nextEpisode()

        override fun raise() = commands.raise()
    }

    private companion object {
        const val BUS_NAME_PREFIX = "org.mpris.MediaPlayer2."

        // Under the app's own name, as the specification asks track paths to be
        const val TRACK_PATH = "/app/katari/episode"

        const val US_PER_MS = 1_000L
    }
}
