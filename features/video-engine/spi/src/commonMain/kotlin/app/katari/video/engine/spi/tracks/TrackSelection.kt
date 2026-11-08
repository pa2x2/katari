package app.katari.video.engine.spi.tracks

/*
 * What to play of each kind of track. Groups and tracks are named by their positions in the player's current
 * VideoTracks; a position that names nothing there plays what the selection would without it.
 */

sealed interface VideoTrackSelection {
    /** The player picks the variant as bandwidth allows. */
    data object Auto : VideoTrackSelection

    /** The player picks as for [Auto], but no taller than [height]. */
    data class MaxHeight(val height: Int) : VideoTrackSelection

    /** Only the variant [trackIndex] of the group [groupIndex]. */
    data class Variant(val groupIndex: Int, val trackIndex: Int) : VideoTrackSelection
}

sealed interface AudioTrackSelection {
    /** The stream's default. */
    data object Default : AudioTrackSelection

    /** The group [groupIndex], all of whose playable tracks stay, so its bitrate variants keep adapting. */
    data class Group(val groupIndex: Int) : AudioTrackSelection
}

sealed interface TextTrackSelection {
    data object Off : TextTrackSelection

    /** The stream's default subtitles, if it marks any. */
    data object Default : TextTrackSelection

    /** The track [trackIndex] of the group [groupIndex]; none when that names no track. */
    data class Track(val groupIndex: Int, val trackIndex: Int) : TextTrackSelection
}
