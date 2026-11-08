package mihon.video.engine.mpv.tracks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One entry of mpv's `track-list` property, with the fields Katari reads. */
@Serializable
internal data class MpvTrack(
    val id: Int,
    val type: String,
    val title: String? = null,
    val lang: String? = null,
    val codec: String? = null,
    val default: Boolean = false,
    val forced: Boolean = false,
    val selected: Boolean = false,
    val external: Boolean = false,
    @SerialName("external-filename") val externalFilename: String? = null,
    /** A picture inside an audio file or a container's cover, which mpv lists as a video track. */
    val albumart: Boolean = false,
    val image: Boolean = false,
    @SerialName("demux-w") val width: Int? = null,
    @SerialName("demux-h") val height: Int? = null,
    @SerialName("demux-fps") val frameRate: Double? = null,
    @SerialName("demux-channel-count") val channelCount: Int? = null,
    @SerialName("demux-samplerate") val sampleRate: Int? = null,
    @SerialName("demux-bitrate") val bitrate: Int? = null,
    /** The variant's bandwidth when the track is one variant of an HLS master playlist or a DASH representation. */
    @SerialName("hls-bitrate") val hlsBitrate: Int? = null,
    /**
     * The HLS variants the track plays in. A variant's own audio is a separate track in each variant, and an
     * alternative rendition is one track in every variant that uses it; DASH tracks have none.
     */
    @SerialName("program-ids") val programIds: List<Int> = emptyList(),
) {
    val kind: MpvTrackKind? get() = MpvTrackKind.entries.firstOrNull { it.mpvName == type }

    /**
     * A subtitle mpv draws itself: ASS in its author's styling, through libass, and picture subtitles, which have no
     * text to hand over. Katari draws every other subtitle from its text.
     */
    val isDrawnByMpv: Boolean get() = kind == MpvTrackKind.Subtitle && codec in MPV_DRAWN_SUBTITLE_CODECS

    /** Whether the track plays in one of [other]'s HLS variants. */
    fun sharesVariantWith(other: MpvTrack): Boolean = programIds.any { it in other.programIds }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val MPV_DRAWN_SUBTITLE_CODECS = setOf(
            "ass",
            "ssa",
            "hdmv_pgs_subtitle",
            "dvd_subtitle",
            "dvb_subtitle",
        )

        /** mpv writes the property as JSON when it is read as a string. */
        fun parseList(text: String): List<MpvTrack> = json.decodeFromString(text)
    }
}

/** The track types mpv selects with `vid`, `aid` and `sid`. */
internal enum class MpvTrackKind(val mpvName: String, val selectProperty: String) {
    Video("video", "vid"),
    Audio("audio", "aid"),
    Subtitle("sub", "sid"),
}
