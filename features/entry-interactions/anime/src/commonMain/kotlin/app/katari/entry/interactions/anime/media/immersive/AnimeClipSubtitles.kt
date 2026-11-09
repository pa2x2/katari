package app.katari.entry.interactions.anime.media.immersive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.tracks.VideoTracks
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.resolvePersistedSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.tracks.applySubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.tracks.availableSubtitleTracks
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A clip's subtitles: the one the player would show for the title, its saved pick or else the source's default, while
 * the feed's subtitles are on. Turning them on where the title keeps them off shows the clip's first subtitle. Whether
 * they are on holds across the feed until the app closes, as its muting does, and a clip never saves a pick.
 */
@Stable
internal class AnimeClipSubtitles(
    private val external: List<VideoSubtitle>,
    savedKey: String?,
    private val preferences: AnimePlayerBasePreferences,
) {
    private var on by mutableStateOf(preferences.immersiveFeedSubtitles)
    private var picked by mutableStateOf(resolvePersistedSubtitleSelection(savedKey, external))
    private var tracks by mutableStateOf(VideoTracks.Empty)

    private val options get() = tracks.availableSubtitleTracks(external)
        .filter { it.selection != VideoPlayerSubtitleSelection.None }

    /** Whether the clip has subtitles to show, its stream's own once the player has opened it. */
    val available: Boolean get() = external.isNotEmpty() || options.isNotEmpty()

    val shown: Boolean get() = on && picked != VideoPlayerSubtitleSelection.None

    val selection: VideoPlayerSubtitleSelection get() = if (on) picked else VideoPlayerSubtitleSelection.None

    fun toggle() {
        if (shown) {
            turn(on = false)
            return
        }
        if (picked == VideoPlayerSubtitleSelection.None) {
            picked = options.firstOrNull()?.selection ?: VideoPlayerSubtitleSelection.Default
        }
        turn(on = true)
    }

    /** Takes up the feed's choice, which another clip may have changed since this one was shown. */
    fun followFeed() {
        on = preferences.immersiveFeedSubtitles
    }

    fun updateTracks(tracks: VideoTracks) {
        this.tracks = tracks
    }

    private fun turn(on: Boolean) {
        this.on = on
        preferences.immersiveFeedSubtitles = on
    }
}

/** The [player]'s subtitles, selected as they change and as the player lists the stream's tracks. */
@Composable
internal fun rememberAnimeClipSubtitles(
    player: VideoPlayer,
    external: List<VideoSubtitle>,
    savedKey: String?,
    preferences: AnimePlayerBasePreferences,
    active: Boolean,
): AnimeClipSubtitles {
    val subtitles = remember(player) { AnimeClipSubtitles(external, savedKey, preferences) }
    LaunchedEffect(subtitles, active) {
        if (active) subtitles.followFeed()
    }
    LaunchedEffect(player, subtitles) {
        // The player adds a stream's subtitles unselected once it opens it, so each new list of tracks is matched again
        val tracks = player.state.map { it.tracks }.distinctUntilChanged()
        combine(snapshotFlow { subtitles.selection }, tracks) { selection, current -> selection to current }
            .collect { (selection, current) ->
                subtitles.updateTracks(current)
                player.applySubtitleSelection(selection)
            }
    }
    return subtitles
}
