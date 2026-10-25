package mihon.video.engine.mpv.tracks

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters

/** What the player asks of each kind of track: off, a track it picked, or for video a maximum height. */
internal data class MpvTrackRequest(
    val disabled: Set<MpvTrackKind> = emptySet(),
    val picked: Map<MpvTrackKind, Int> = emptyMap(),
    val maxVideoHeight: Int = Int.MAX_VALUE,
)

internal fun TrackSelectionParameters.toMpvTrackRequest(): MpvTrackRequest = MpvTrackRequest(
    disabled = MpvTrackKind.entries.filterTo(mutableSetOf()) { it.media3Type in disabledTrackTypes },
    picked = MpvTrackKind.entries.mapNotNull { kind ->
        overrides.values
            .firstOrNull { it.type == kind.media3Type }
            ?.let { override -> mpvTrackIdOf(override.mediaTrackGroup) }
            ?.let { kind to it }
    }.toMap(),
    maxVideoHeight = maxVideoHeight,
)

/**
 * What to set mpv's `vid`, `aid` and `sid` to for [request]: "no" when the kind is off, the picked track, the tallest
 * video track within the maximum video height, [autoVideo] when Katari picks Auto's quality, or mpv's own choice.
 *
 * Each HLS variant carries its own copy of the audio, so the audio follows the video into its variant: the audio
 * picked, or the one playing, becomes its copy in the chosen video's variant. Otherwise mpv would download both
 * variants.
 */
internal fun mpvTrackChoices(
    request: MpvTrackRequest,
    tracks: List<MpvTrack>,
    autoVideo: Int? = null,
): Map<MpvTrackKind, String> {
    val video = videoChoice(request, tracks, autoVideo)
    val videoTrack = (video.toIntOrNull() ?: tracks.selected(MpvTrackKind.Video)?.id)
        ?.let { id -> tracks.track(MpvTrackKind.Video, id) }
    return mapOf(
        MpvTrackKind.Video to video,
        MpvTrackKind.Audio to audioChoice(request, tracks, videoTrack),
        MpvTrackKind.Subtitle to (request.offOrPicked(MpvTrackKind.Subtitle) ?: AUTO),
    )
}

private fun videoChoice(request: MpvTrackRequest, tracks: List<MpvTrack>, autoVideo: Int?): String {
    request.offOrPicked(MpvTrackKind.Video)?.let { return it }
    if (request.maxVideoHeight != Int.MAX_VALUE) {
        val heights = tracks.mapNotNull { track ->
            track.height?.takeIf { track.kind == MpvTrackKind.Video && it > 0 }?.let { track.id to it }
        }
        val chosen = heights.filter { (_, height) -> height <= request.maxVideoHeight }.maxByOrNull { it.second }
            ?: heights.minByOrNull { it.second }
        chosen?.let { (id, _) -> return id.toString() }
    }
    return autoVideo?.toString() ?: AUTO
}

private fun audioChoice(request: MpvTrackRequest, tracks: List<MpvTrack>, video: MpvTrack?): String {
    if (MpvTrackKind.Audio in request.disabled) return NONE
    val pickedId = request.picked[MpvTrackKind.Audio]
    val picked = pickedId?.let { id -> tracks.track(MpvTrackKind.Audio, id) }
    val audio = picked ?: tracks.selected(MpvTrackKind.Audio)
    val paired = if (video != null && audio != null) tracks.copyIn(video, audio) else audio
    // Audio of a variant stays named: "auto" would have mpv choose again, and it may choose another rendition.
    return when {
        paired == null -> pickedId?.toString() ?: AUTO
        picked != null || paired.programIds.isNotEmpty() -> paired.id.toString()
        else -> AUTO
    }
}

/** [audio]'s copy in [video]'s HLS variant: the one of the same language and title, else the variant's first. */
private fun List<MpvTrack>.copyIn(video: MpvTrack, audio: MpvTrack): MpvTrack {
    if (video.programIds.isEmpty() || audio.programIds.isEmpty() || audio.sharesVariantWith(video)) return audio
    val inVariant = filter { it.kind == MpvTrackKind.Audio && !it.external && it.sharesVariantWith(video) }
    return inVariant.firstOrNull { it.lang == audio.lang && it.title == audio.title }
        ?: inVariant.firstOrNull()
        ?: audio
}

private fun MpvTrackRequest.offOrPicked(kind: MpvTrackKind): String? =
    if (kind in disabled) NONE else picked[kind]?.toString()

private fun List<MpvTrack>.selected(kind: MpvTrackKind): MpvTrack? = firstOrNull { it.kind == kind && it.selected }

private fun List<MpvTrack>.track(kind: MpvTrackKind, id: Int): MpvTrack? =
    firstOrNull { it.kind == kind && it.id == id }

private val MpvTrackKind.media3Type: Int
    get() = when (this) {
        MpvTrackKind.Video -> C.TRACK_TYPE_VIDEO
        MpvTrackKind.Audio -> C.TRACK_TYPE_AUDIO
        MpvTrackKind.Subtitle -> C.TRACK_TYPE_TEXT
    }

private const val AUTO = "auto"
private const val NONE = "no"
