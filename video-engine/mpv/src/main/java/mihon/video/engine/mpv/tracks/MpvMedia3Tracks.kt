package mihon.video.engine.mpv.tracks

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.google.common.collect.ImmutableList
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import mihon.video.engine.spi.tracks.audioChoiceKey
import mihon.video.engine.spi.tracks.subtitleChoiceKey

/** A source's subtitle as mpv opens it: [url] is what mpv was given, and names the track it lists for it. */
internal data class MpvExternalSubtitle(
    val index: Int,
    val url: String,
    val subtitle: VideoSubtitle,
)

/** A stream's external audio track as mpv opens it: [url] is what mpv was given, and names the track it lists for it. */
internal data class MpvExternalAudio(
    val index: Int,
    val url: String,
    val track: VideoAudioTrack,
)

/**
 * mpv's tracks as Media3 [Tracks], one group per track, so the player's track code reads and overrides them as it
 * does ExoPlayer's. A group's id names its mpv track. An external subtitle's format takes its id, label, language and
 * flags from its [VideoSubtitle], as ExoPlayer's does, which is how the player tells it from an embedded one. An
 * external audio track's format is numbered as ExoPlayer numbers merged audio, which is how the player names it.
 */
internal fun mpvMedia3Tracks(
    tracks: List<MpvTrack>,
    externalSubtitles: List<MpvExternalSubtitle>,
    externalAudio: List<MpvExternalAudio>,
): Tracks {
    val subtitlesByUrl = externalSubtitles.associateBy { it.url }
    val audioByUrl = externalAudio.associateBy { it.url }
    val externalSubtitleTracks = tracks.filter { it.kind == MpvTrackKind.Subtitle && it.external }
    val externalAudioTracks = tracks.filter { it.kind == MpvTrackKind.Audio && it.external }
    val groups = tracks.mapNotNull { track ->
        val kind = track.kind ?: return@mapNotNull null
        if (track.albumart || track.image) return@mapNotNull null
        // mpv lists external files in the order it opened them, which is the order they were given in.
        val format = when {
            !track.external -> track.format(kind)
            kind == MpvTrackKind.Subtitle ->
                (
                    track.externalFilename?.let(subtitlesByUrl::get)
                        ?: externalSubtitles.getOrNull(externalSubtitleTracks.indexOf(track))
                    )?.format(track)
            kind == MpvTrackKind.Audio ->
                (
                    track.externalFilename?.let(audioByUrl::get)
                        ?: externalAudio.getOrNull(externalAudioTracks.indexOf(track))
                    )?.format(track)
            else -> null
        } ?: track.format(kind)
        Tracks.Group(
            TrackGroup(groupId(track.id), format),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(track.selected),
        )
    }
    return Tracks(ImmutableList.copyOf(groups))
}

/**
 * What to set mpv's [kind] selection to for [parameters]: "no" when the type is disabled, the track of an override,
 * the tallest video track within the maximum video height, or mpv's own choice.
 */
internal fun mpvTrackChoice(kind: MpvTrackKind, parameters: TrackSelectionParameters, tracks: List<MpvTrack>): String {
    val type = kind.media3Type
    if (type in parameters.disabledTrackTypes) return "no"
    parameters.overrides.values
        .firstOrNull { it.type == type }
        ?.let { override -> trackIdOf(override.mediaTrackGroup)?.let { return it.toString() } }
    if (kind == MpvTrackKind.Video && parameters.maxVideoHeight != Int.MAX_VALUE) {
        val heights = tracks.mapNotNull { track ->
            track.height?.takeIf { track.kind == MpvTrackKind.Video && it > 0 }?.let { track.id to it }
        }
        val chosen = heights.filter { (_, height) -> height <= parameters.maxVideoHeight }.maxByOrNull { it.second }
            ?: heights.minByOrNull { it.second }
        chosen?.let { (id, _) -> return id.toString() }
    }
    return "auto"
}

private val MpvTrackKind.media3Type: Int
    get() = when (this) {
        MpvTrackKind.Video -> C.TRACK_TYPE_VIDEO
        MpvTrackKind.Audio -> C.TRACK_TYPE_AUDIO
        MpvTrackKind.Subtitle -> C.TRACK_TYPE_TEXT
    }

private const val GROUP_ID_PREFIX = "mpv:"

private fun groupId(trackId: Int) = "$GROUP_ID_PREFIX$trackId"

private fun trackIdOf(group: TrackGroup): Int? =
    group.id.takeIf { it.startsWith(GROUP_ID_PREFIX) }?.removePrefix(GROUP_ID_PREFIX)?.toIntOrNull()

private fun MpvTrack.format(kind: MpvTrackKind): Format = Format.Builder()
    .setId(groupId(id))
    .setLabel(title)
    .setLanguage(lang)
    .setSelectionFlags(selectionFlags(default, forced))
    .setCodecs(codec)
    .setSampleMimeType(sampleMimeType(kind, codec))
    .apply {
        when (kind) {
            MpvTrackKind.Video -> {
                width?.let(::setWidth)
                height?.let(::setHeight)
                frameRate?.let { setFrameRate(it.toFloat()) }
                (hlsBitrate ?: bitrate)?.let(::setAverageBitrate)
            }
            MpvTrackKind.Audio -> {
                channelCount?.let(::setChannelCount)
                sampleRate?.let(::setSampleRate)
                (hlsBitrate ?: bitrate)?.let(::setAverageBitrate)
            }
            MpvTrackKind.Subtitle -> setRoleFlags(C.ROLE_FLAG_SUBTITLE)
        }
    }
    .build()

private fun MpvExternalSubtitle.format(track: MpvTrack): Format = Format.Builder()
    // The `<n>:<key>` form ExoPlayer gives an external subtitle's track.
    .setId("$index:${subtitleChoiceKey(subtitle)}")
    .setLabel(subtitle.label)
    .setLanguage(subtitle.language)
    .setSelectionFlags(selectionFlags(subtitle.isDefault, subtitle.isForced))
    .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
    .setCodecs(track.codec)
    .setSampleMimeType(subtitle.mimeType ?: sampleMimeType(MpvTrackKind.Subtitle, track.codec))
    .build()

private fun MpvExternalAudio.format(mpvTrack: MpvTrack): Format = Format.Builder()
    // The `<n>:` form ExoPlayer gives the n-th source merged after the stream.
    .setId("${index + 1}:${audioChoiceKey(track)}")
    .setLabel(track.label)
    .setLanguage(track.language)
    .setSelectionFlags(selectionFlags(track.isDefault, isForced = false))
    .setCodecs(mpvTrack.codec)
    .setSampleMimeType(sampleMimeType(MpvTrackKind.Audio, mpvTrack.codec))
    .apply {
        mpvTrack.channelCount?.let(::setChannelCount)
        mpvTrack.sampleRate?.let(::setSampleRate)
    }
    .build()

private fun selectionFlags(isDefault: Boolean, isForced: Boolean): Int =
    (if (isDefault) C.SELECTION_FLAG_DEFAULT else 0) or (if (isForced) C.SELECTION_FLAG_FORCED else 0)

/** Media3 reads a track's type from its MIME type, so an unknown codec still gets one of the right base type. */
private fun sampleMimeType(kind: MpvTrackKind, codec: String?): String {
    val known = when (kind) {
        MpvTrackKind.Video -> VIDEO_MIME_TYPES[codec]
        MpvTrackKind.Audio -> AUDIO_MIME_TYPES[codec]
        MpvTrackKind.Subtitle -> SUBTITLE_MIME_TYPES[codec]
    }
    val baseType = when (kind) {
        MpvTrackKind.Video -> MimeTypes.BASE_TYPE_VIDEO
        MpvTrackKind.Audio -> MimeTypes.BASE_TYPE_AUDIO
        MpvTrackKind.Subtitle -> MimeTypes.BASE_TYPE_TEXT
    }
    return known ?: "$baseType/x-${codec ?: "unknown"}"
}

// Keyed by FFmpeg's codec names, which mpv reports.
private val VIDEO_MIME_TYPES = mapOf(
    "h264" to MimeTypes.VIDEO_H264,
    "hevc" to MimeTypes.VIDEO_H265,
    "av1" to MimeTypes.VIDEO_AV1,
    "vp8" to MimeTypes.VIDEO_VP8,
    "vp9" to MimeTypes.VIDEO_VP9,
    "mpeg4" to MimeTypes.VIDEO_MP4V,
    "mpeg2video" to MimeTypes.VIDEO_MPEG2,
)

private val AUDIO_MIME_TYPES = mapOf(
    "aac" to MimeTypes.AUDIO_AAC,
    "mp3" to MimeTypes.AUDIO_MPEG,
    "opus" to MimeTypes.AUDIO_OPUS,
    "vorbis" to MimeTypes.AUDIO_VORBIS,
    "flac" to MimeTypes.AUDIO_FLAC,
    "ac3" to MimeTypes.AUDIO_AC3,
    "eac3" to MimeTypes.AUDIO_E_AC3,
    "dts" to MimeTypes.AUDIO_DTS,
    "truehd" to MimeTypes.AUDIO_TRUEHD,
)

private val SUBTITLE_MIME_TYPES = mapOf(
    "ass" to MimeTypes.TEXT_SSA,
    "ssa" to MimeTypes.TEXT_SSA,
    "subrip" to MimeTypes.APPLICATION_SUBRIP,
    "webvtt" to MimeTypes.TEXT_VTT,
    "mov_text" to MimeTypes.APPLICATION_TX3G,
    "hdmv_pgs_subtitle" to MimeTypes.APPLICATION_PGS,
    "dvd_subtitle" to MimeTypes.APPLICATION_VOBSUB,
    "dvb_subtitle" to MimeTypes.APPLICATION_DVBSUBS,
)
