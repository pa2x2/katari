package mihon.video.engine.spi.tracks

/**
 * The tracks of what a player plays, grouped as the engine groups them. A group's position in [groups] is part of its
 * identity: selections name groups by it, and saved subtitle choices from before fingerprints did too, so groups of
 * every kind are listed, in the engine's order.
 */
data class VideoTracks(val groups: List<VideoTrackGroup>) {
    fun containsKind(kind: TrackKind): Boolean = groups.any { it.kind == kind }

    companion object {
        val Empty = VideoTracks(emptyList())
    }
}

/**
 * Tracks the player switches between as one: the variants of an [adaptive] video stream, or a single audio or text
 * track. [id] is the engine's own id for the group.
 */
data class VideoTrackGroup(
    val kind: TrackKind,
    val id: String,
    val adaptive: Boolean,
    val tracks: List<VideoTrack>,
) {
    /** At least one of its tracks can be played. */
    val supported: Boolean get() = tracks.any(VideoTrack::supported)

    val selected: Boolean get() = tracks.any(VideoTrack::selected)
}

enum class TrackKind {
    Video,
    Audio,
    Text,

    /** Anything else, including a track in a format the engine doesn't know, whose kind it can't tell. */
    Other,
}

/**
 * One track. [id] is stable for a stream: the tracks an engine loads for the stream's external subtitles and audio
 * tracks carry the ids [subtitleChoiceKey] and [externalAudioTrackOf] read. [mimeType] uses Media3's names, which mpv's
 * codecs are translated to. [height] is null where the track doesn't say, as for audio. [supported] is whether the
 * engine can play it.
 */
data class VideoTrack(
    val id: String?,
    val label: String?,
    val language: String?,
    val isDefault: Boolean,
    val isForced: Boolean,
    val mimeType: String?,
    val codecs: String?,
    val height: Int?,
    val supported: Boolean,
    val selected: Boolean,
) {
    // An engine that converts subtitles while extracting reports the format it converted to, with the original in
    // codecs, as ExoPlayer does.
    private fun hasFormat(formats: Set<String>): Boolean = mimeType in formats || codecs in formats

    /** ASS/SSA, which carries its own styling. */
    val isStyledSubtitle: Boolean get() = hasFormat(STYLED_SUBTITLE_TYPES)

    /** PGS, VobSub or DVB: subtitles made of pictures. */
    val isBitmapSubtitle: Boolean get() = hasFormat(BITMAP_SUBTITLE_TYPES)

    private companion object {
        val STYLED_SUBTITLE_TYPES = setOf("text/x-ssa")
        val BITMAP_SUBTITLE_TYPES = setOf("application/pgs", "application/vobsub", "application/dvbsubs")
    }
}
