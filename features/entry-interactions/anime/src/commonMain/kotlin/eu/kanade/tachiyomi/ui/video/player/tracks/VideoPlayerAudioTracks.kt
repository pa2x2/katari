package eu.kanade.tachiyomi.ui.video.player.tracks

import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.tracks.AudioTrackSelection
import mihon.video.engine.spi.tracks.TrackKind
import mihon.video.engine.spi.tracks.VideoTrackGroup
import mihon.video.engine.spi.tracks.VideoTracks
import mihon.video.engine.spi.tracks.externalAudioTrackOf
import java.util.Locale

/**
 * An audio track of the playing stream: one carried inside it, such as a language of an HLS playlist, or one of its
 * external audio tracks. A source's dubs are separate streams picked in the Dub row; these switch without reloading.
 * Each option is a whole track group, so the bitrate variants of one language stay adaptive.
 *
 * [key] comes from the track's language and label rather than its position, so a pick finds the same track in the
 * next episode of a source, and in the other engine. [label] is null when the track names neither, and the UI numbers
 * it by [number] instead. [needsMpv] marks a track the playing engine can't decode, which only mpv can play.
 */
internal data class VideoPlayerAudioTrackOption(
    val key: String,
    val label: String?,
    val number: Int,
    val selected: Boolean,
    val needsMpv: Boolean = false,
)

/** [externalAudio] is the playing stream's external audio tracks, which name the tracks the engine loaded for them. */
internal fun VideoTracks.availableAudioTracks(externalAudio: List<VideoAudioTrack>): List<VideoPlayerAudioTrackOption> {
    val seenKeys = mutableMapOf<String, Int>()
    return audioGroups().mapIndexed { index, (_, group) ->
        val groupTrack = group.tracks[group.tracks.indexOfFirst { it.supported }.coerceAtLeast(0)]
        val track = externalAudioTrackOf(groupTrack.id, externalAudio)
            ?.let { groupTrack.copy(label = it.label, language = it.language ?: groupTrack.language) }
            ?: groupTrack
        val language = track.language?.takeUnless { it.isBlank() || it.equals("und", ignoreCase = true) }
        val baseKey = "${language.orEmpty()}|${track.label.orEmpty()}"
        // Groups that name themselves alike still need distinct keys.
        val duplicate = seenKeys.getOrDefault(baseKey, 0)
        seenKeys[baseKey] = duplicate + 1
        VideoPlayerAudioTrackOption(
            key = if (duplicate == 0) baseKey else "$baseKey#$duplicate",
            label = track.label?.takeIf { it.isNotBlank() }
                ?: language?.let { Locale.forLanguageTag(it).displayLanguage.takeIf(String::isNotBlank) },
            number = index + 1,
            selected = group.selected,
            needsMpv = !group.supported,
        )
    }
}

/**
 * Plays the audio track [key] names. When it is null, or this stream has no such track the player can play, the
 * external track the source marks as default plays, else the stream's own default.
 */
internal fun VideoPlayer.applyAudioTrack(key: String?, externalAudio: List<VideoAudioTrack>) {
    val tracks = state.value.tracks
    val groups = tracks.audioGroups()
    val index = key?.let { wanted -> tracks.availableAudioTracks(externalAudio).indexOfFirst { it.key == wanted } }
        ?.takeIf { it >= 0 && groups[it].value.supported }
        ?: groups.indexOfFirst { (_, group) ->
            group.supported && externalAudioTrackOf(group.tracks[0].id, externalAudio)?.isDefault == true
        }
    selectAudio(
        groups.getOrNull(index)?.let { AudioTrackSelection.Group(it.index) } ?: AudioTrackSelection.Default,
    )
}

/** The audio groups with their positions among all of [VideoTracks.groups]. */
private fun VideoTracks.audioGroups(): List<IndexedValue<VideoTrackGroup>> = groups.withIndex().filter { (_, group) ->
    group.kind == TrackKind.Audio && group.tracks.isNotEmpty()
}
