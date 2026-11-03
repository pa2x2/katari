package eu.kanade.tachiyomi.ui.video.player.tracks

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
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
internal fun Player.availableAudioTracks(externalAudio: List<VideoAudioTrack>): List<VideoPlayerAudioTrackOption> {
    val seenKeys = mutableMapOf<String, Int>()
    return audioGroups().mapIndexed { index, group ->
        val trackFormat = group.getTrackFormat(group.supportedTrackIndices().firstOrNull() ?: 0)
        val format = externalAudioTrackOf(trackFormat.id, externalAudio)
            ?.let {
                trackFormat.buildUpon().setLabel(it.label).setLanguage(it.language ?: trackFormat.language).build()
            }
            ?: trackFormat
        val language = format.language?.takeUnless { it.isBlank() || it.equals("und", ignoreCase = true) }
        val baseKey = "${language.orEmpty()}|${format.label.orEmpty()}"
        // Groups that name themselves alike still need distinct keys.
        val duplicate = seenKeys.getOrDefault(baseKey, 0)
        seenKeys[baseKey] = duplicate + 1
        VideoPlayerAudioTrackOption(
            key = if (duplicate == 0) baseKey else "$baseKey#$duplicate",
            label = format.label?.takeIf { it.isNotBlank() }
                ?: language?.let { Locale.forLanguageTag(it).displayLanguage.takeIf(String::isNotBlank) },
            number = index + 1,
            selected = group.isSelected,
            needsMpv = !group.isPlayable(),
        )
    }
}

/**
 * Plays the audio track [key] names. When it is null, or this stream has no such track the player can play, the
 * external track the source marks as default plays, else the stream's own default.
 */
internal fun Player.applyAudioTrack(key: String?, externalAudio: List<VideoAudioTrack>) {
    val groups = audioGroups()
    val index = key?.let { wanted -> availableAudioTracks(externalAudio).indexOfFirst { it.key == wanted } }
        ?.takeIf { it >= 0 && groups[it].isPlayable() }
        ?: groups.indexOfFirst {
            it.isPlayable() && externalAudioTrackOf(it.getTrackFormat(0).id, externalAudio)?.isDefault == true
        }
    val builder = trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO)
    groups.getOrNull(index)?.let { group ->
        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, group.supportedTrackIndices()))
    }
    trackSelectionParameters = builder.build()
}

private fun Player.audioGroups(): List<Tracks.Group> = currentTracks.groups.filter {
    it.type == C.TRACK_TYPE_AUDIO && it.length > 0
}

private fun Tracks.Group.isPlayable(): Boolean = isSupported && supportedTrackIndices().isNotEmpty()

private fun Tracks.Group.supportedTrackIndices(): List<Int> = (0 until length).filter(::isTrackSupported)
