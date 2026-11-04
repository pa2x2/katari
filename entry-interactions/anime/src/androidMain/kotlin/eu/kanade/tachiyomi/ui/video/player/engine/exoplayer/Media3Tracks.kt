package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import mihon.video.engine.spi.tracks.AudioTrackSelection
import mihon.video.engine.spi.tracks.TextTrackSelection
import mihon.video.engine.spi.tracks.TrackKind
import mihon.video.engine.spi.tracks.VideoTrack
import mihon.video.engine.spi.tracks.VideoTrackGroup
import mihon.video.engine.spi.tracks.VideoTrackSelection
import mihon.video.engine.spi.tracks.VideoTracks

internal fun Tracks.toVideoTracks(): VideoTracks = VideoTracks(
    groups.map { group ->
        VideoTrackGroup(
            kind = when (group.type) {
                C.TRACK_TYPE_VIDEO -> TrackKind.Video
                C.TRACK_TYPE_AUDIO -> TrackKind.Audio
                C.TRACK_TYPE_TEXT -> TrackKind.Text
                else -> TrackKind.Other
            },
            id = group.mediaTrackGroup.id,
            adaptive = group.isAdaptiveSupported,
            tracks = (0 until group.length).map { index ->
                group.getTrackFormat(index).toVideoTrack(
                    supported = group.isTrackSupported(index),
                    selected = group.isTrackSelected(index),
                )
            },
        )
    },
)

private fun Format.toVideoTrack(supported: Boolean, selected: Boolean) = VideoTrack(
    id = id,
    label = label,
    language = language,
    isDefault = selectionFlags and C.SELECTION_FLAG_DEFAULT != 0,
    isForced = selectionFlags and C.SELECTION_FLAG_FORCED != 0,
    mimeType = sampleMimeType,
    codecs = codecs,
    height = height.takeIf { it != Format.NO_VALUE },
    supported = supported,
    selected = selected,
)

internal fun TrackSelectionParameters.with(selection: VideoTrackSelection, tracks: Tracks): TrackSelectionParameters {
    val builder = buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setForceLowestBitrate(false)
        .setForceHighestSupportedBitrate(false)
    when (selection) {
        VideoTrackSelection.Auto -> builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        is VideoTrackSelection.MaxHeight -> builder.setMaxVideoSize(Int.MAX_VALUE, selection.height)
        is VideoTrackSelection.Variant -> tracks.override(selection.groupIndex, listOf(selection.trackIndex))
            ?.let(builder::setOverrideForType)
    }
    return builder.build()
}

internal fun TrackSelectionParameters.with(selection: AudioTrackSelection, tracks: Tracks): TrackSelectionParameters {
    val builder = buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO)
    if (selection is AudioTrackSelection.Group) {
        val group = tracks.groups.getOrNull(selection.groupIndex)
        val supported = group?.let { (0 until it.length).filter(it::isTrackSupported) }.orEmpty()
        tracks.override(selection.groupIndex, supported)?.let(builder::setOverrideForType)
    }
    return builder.build()
}

internal fun TrackSelectionParameters.with(selection: TextTrackSelection, tracks: Tracks): TrackSelectionParameters {
    val track = selection as? TextTrackSelection.Track
    val override = track?.let { tracks.override(it.groupIndex, listOf(it.trackIndex)) }
    val disabled = selection == TextTrackSelection.Off || (track != null && override == null)
    val builder = buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, disabled)
        .setPreferredTextLanguage(null)
    override?.let(builder::setOverrideForType)
    return builder.build()
}

private fun Tracks.override(groupIndex: Int, trackIndices: List<Int>): TrackSelectionOverride? {
    val group = groups.getOrNull(groupIndex) ?: return null
    if (trackIndices.isEmpty() || trackIndices.any { it !in 0 until group.length }) return null
    return TrackSelectionOverride(group.mediaTrackGroup, trackIndices)
}
