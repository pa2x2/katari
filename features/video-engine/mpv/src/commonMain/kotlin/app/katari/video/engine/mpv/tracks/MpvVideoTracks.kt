package app.katari.video.engine.mpv.tracks

import app.katari.video.engine.mpv.proxy.hls.HlsVariantSet
import app.katari.video.engine.spi.tracks.TrackKind
import app.katari.video.engine.spi.tracks.VideoTrack
import app.katari.video.engine.spi.tracks.VideoTrackGroup
import app.katari.video.engine.spi.tracks.VideoTracks
import app.katari.video.engine.spi.tracks.audioChoiceKey
import app.katari.video.engine.spi.tracks.subtitleChoiceKey
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoSubtitle

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
 * mpv's tracks as the player's [VideoTracks], one group per track, read and picked as ExoPlayer's are. A group's id
 * names its mpv track ([mpvTrackIdOf]). An external subtitle takes its id, label, language and flags from its
 * [VideoSubtitle], as ExoPlayer's does, which is how the player tells it from an embedded one. An external audio track
 * is numbered as ExoPlayer numbers merged audio, which is how the player names it. Languages are normalised as
 * ExoPlayer's are ([normalizedLanguageTag]), so saved subtitle choices match on both engines.
 *
 * Of an HLS stream's audio, only the copies in the playing video's variant are listed: the others are the same audio
 * in another quality, which [mpvTrackChoices] swaps in when the video changes.
 *
 * The video track of a set of variants the proxy switches between ([variantSets]) is one group with a track per
 * variant, as ExoPlayer lists an HLS stream's variants; the one selected is the variant playing at [positionMs].
 */
internal fun mpvVideoTracks(
    tracks: List<MpvTrack>,
    externalSubtitles: List<MpvExternalSubtitle>,
    externalAudio: List<MpvExternalAudio>,
    variantSets: List<HlsVariantSet>,
    positionMs: Long,
): VideoTracks {
    val subtitlesByUrl = externalSubtitles.associateBy { it.url }
    val audioByUrl = externalAudio.associateBy { it.url }
    val externalSubtitleTracks = tracks.filter { it.kind == MpvTrackKind.Subtitle && it.external }
    val externalAudioTracks = tracks.filter { it.kind == MpvTrackKind.Audio && it.external }
    val playingVideo = tracks.firstOrNull { it.kind == MpvTrackKind.Video && it.selected }
    val groups = tracks.mapNotNull { track ->
        val kind = track.kind ?: return@mapNotNull null
        if (track.albumart || track.image) return@mapNotNull null
        if (kind == MpvTrackKind.Audio && playingVideo != null && track.inOtherVariantThan(playingVideo)) {
            return@mapNotNull null
        }
        if (kind == MpvTrackKind.Video) {
            variantSets.firstOrNull { it.programId in track.programIds }?.let { set ->
                return@mapNotNull track.variantSetGroup(set, positionMs)
            }
        }
        // mpv lists external files in the order it opened them, which is the order they were given in.
        val published = when {
            !track.external -> track.published(kind)
            kind == MpvTrackKind.Subtitle ->
                (
                    track.externalFilename?.let(subtitlesByUrl::get)
                        ?: externalSubtitles.getOrNull(externalSubtitleTracks.indexOf(track))
                    )?.published(track)
            kind == MpvTrackKind.Audio ->
                (
                    track.externalFilename?.let(audioByUrl::get)
                        ?: externalAudio.getOrNull(externalAudioTracks.indexOf(track))
                    )?.published(track)
            else -> null
        } ?: track.published(kind)
        VideoTrackGroup(kind = kind.trackKind, id = groupId(track.id), adaptive = false, tracks = listOf(published))
    }
    return VideoTracks(groups)
}

private const val GROUP_ID_PREFIX = "mpv:"

private fun groupId(trackId: Int) = "$GROUP_ID_PREFIX$trackId"

/** The mpv track a group of [mpvVideoTracks] stands for. */
internal fun mpvTrackIdOf(group: VideoTrackGroup): Int? =
    group.id.takeIf { it.startsWith(GROUP_ID_PREFIX) }?.removePrefix(GROUP_ID_PREFIX)?.toIntOrNull()

private fun MpvTrack.variantSetGroup(set: HlsVariantSet, positionMs: Long): VideoTrackGroup {
    val playing = set.variantAt(positionMs).takeIf { selected }
    val variants = set.variants.mapIndexed { index, loaded ->
        val variant = loaded.variant
        VideoTrack(
            id = "${groupId(id)}:$index",
            label = null,
            language = null,
            isDefault = false,
            isForced = false,
            mimeType = sampleMimeType(MpvTrackKind.Video, codec),
            codecs = variant.codecs?.joinToString(","),
            height = variant.height,
            supported = true,
            selected = index == playing,
        )
    }
    return VideoTrackGroup(
        kind = TrackKind.Video,
        id = groupId(id),
        adaptive = variants.size > 1,
        tracks = variants,
    )
}

private fun MpvTrack.inOtherVariantThan(video: MpvTrack): Boolean =
    programIds.isNotEmpty() && video.programIds.isNotEmpty() && !sharesVariantWith(video)

private fun MpvTrack.published(kind: MpvTrackKind) = VideoTrack(
    id = groupId(id),
    label = title,
    language = normalizedLanguageTag(lang),
    isDefault = default,
    isForced = forced,
    mimeType = sampleMimeType(kind, codec),
    codecs = codec,
    height = height.takeIf { kind == MpvTrackKind.Video },
    supported = true,
    selected = selected,
)

private fun MpvExternalSubtitle.published(track: MpvTrack) = VideoTrack(
    // The `<n>:<key>` form ExoPlayer gives an external subtitle's track.
    id = "$index:${subtitleChoiceKey(subtitle)}",
    label = subtitle.label,
    language = normalizedLanguageTag(subtitle.language),
    isDefault = subtitle.isDefault,
    isForced = subtitle.isForced,
    mimeType = subtitle.mimeType ?: sampleMimeType(MpvTrackKind.Subtitle, track.codec),
    codecs = track.codec,
    height = null,
    supported = true,
    selected = track.selected,
)

private fun MpvExternalAudio.published(mpvTrack: MpvTrack) = VideoTrack(
    // The `<n>:` form ExoPlayer gives the n-th source merged after the stream.
    id = "${index + 1}:${audioChoiceKey(track)}",
    label = track.label,
    language = normalizedLanguageTag(track.language),
    isDefault = track.isDefault,
    isForced = false,
    mimeType = sampleMimeType(MpvTrackKind.Audio, mpvTrack.codec),
    codecs = mpvTrack.codec,
    height = null,
    supported = true,
    selected = mpvTrack.selected,
)

private val MpvTrackKind.trackKind: TrackKind
    get() = when (this) {
        MpvTrackKind.Video -> TrackKind.Video
        MpvTrackKind.Audio -> TrackKind.Audio
        MpvTrackKind.Subtitle -> TrackKind.Text
    }

/**
 * The MIME type ExoPlayer gives a track of [codec], in Media3's names, which the player reads subtitle formats by; an
 * unknown codec still gets one of the right base type.
 */
private fun sampleMimeType(kind: MpvTrackKind, codec: String?): String {
    val known = when (kind) {
        MpvTrackKind.Video -> VIDEO_MIME_TYPES[codec]
        MpvTrackKind.Audio -> AUDIO_MIME_TYPES[codec]
        MpvTrackKind.Subtitle -> SUBTITLE_MIME_TYPES[codec]
    }
    val baseType = when (kind) {
        MpvTrackKind.Video -> "video"
        MpvTrackKind.Audio -> "audio"
        MpvTrackKind.Subtitle -> "text"
    }
    return known ?: "$baseType/x-${codec ?: "unknown"}"
}

// Keyed by FFmpeg's codec names, which mpv reports.
private val VIDEO_MIME_TYPES = mapOf(
    "h264" to "video/avc",
    "hevc" to "video/hevc",
    "av1" to "video/av01",
    "vp8" to "video/x-vnd.on2.vp8",
    "vp9" to "video/x-vnd.on2.vp9",
    "mpeg4" to "video/mp4v-es",
    "mpeg2video" to "video/mpeg2",
)

private val AUDIO_MIME_TYPES = mapOf(
    "aac" to "audio/mp4a-latm",
    "mp3" to "audio/mpeg",
    "opus" to "audio/opus",
    "vorbis" to "audio/vorbis",
    "flac" to "audio/flac",
    "ac3" to "audio/ac3",
    "eac3" to "audio/eac3",
    "dts" to "audio/vnd.dts",
    "truehd" to "audio/true-hd",
)

private val SUBTITLE_MIME_TYPES = mapOf(
    "ass" to "text/x-ssa",
    "ssa" to "text/x-ssa",
    "subrip" to "application/x-subrip",
    "webvtt" to "text/vtt",
    "mov_text" to "application/x-quicktime-tx3g",
    "hdmv_pgs_subtitle" to "application/pgs",
    "dvd_subtitle" to "application/vobsub",
    "dvb_subtitle" to "application/dvbsubs",
)
