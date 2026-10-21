package eu.kanade.tachiyomi.ui.video.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

/**
 * An audio track carried inside the playing stream, such as one language of an HLS playlist. A source's dubs are
 * separate streams picked in the Dub row; these switch without reloading. Each option is a whole track group, so the
 * bitrate variants of one language stay adaptive.
 *
 * [key] comes from the track's language and label rather than its position, so a pick finds the same track in the
 * next episode of a source. [label] is null when the track names neither, and the UI numbers it by [number] instead.
 */
internal data class VideoPlayerAudioTrackOption(
    val key: String,
    val label: String?,
    val number: Int,
    val selected: Boolean,
)

internal fun ExoPlayer.availableAudioTracks(): List<VideoPlayerAudioTrackOption> {
    val seenKeys = mutableMapOf<String, Int>()
    return audioGroups().mapIndexed { index, group ->
        val format = group.getTrackFormat(group.supportedTrackIndices().first())
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
        )
    }
}

/** Plays the audio track [key] names, or the stream's own default when it is null or this stream has no such track. */
internal fun ExoPlayer.applyAudioTrack(key: String?) {
    val index = key?.let { wanted -> availableAudioTracks().indexOfFirst { it.key == wanted } } ?: -1
    val builder = trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO)
    audioGroups().getOrNull(index)?.let { group ->
        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, group.supportedTrackIndices()))
    }
    trackSelectionParameters = builder.build()
}

private fun ExoPlayer.audioGroups(): List<Tracks.Group> = currentTracks.groups.filter {
    it.type == C.TRACK_TYPE_AUDIO && it.isSupported && it.supportedTrackIndices().isNotEmpty()
}

private fun Tracks.Group.supportedTrackIndices(): List<Int> = (0 until length).filter(::isTrackSupported)
