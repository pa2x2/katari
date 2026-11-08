package mihon.video.engine.mpv.tracks

import mihon.video.engine.mpv.proxy.hls.HlsVariantPick
import mihon.video.engine.spi.tracks.AudioTrackSelection
import mihon.video.engine.spi.tracks.TextTrackSelection
import mihon.video.engine.spi.tracks.TrackKind
import mihon.video.engine.spi.tracks.VideoTrackGroup
import mihon.video.engine.spi.tracks.VideoTrackSelection
import mihon.video.engine.spi.tracks.VideoTracks

/**
 * What the player asks of each kind of track: off, a track it picked, or for video a maximum height. A picked video
 * track that is a set of variants comes with the variant picked, [pickedVideoVariant]. Each kind's selection replaces
 * what was asked of that kind before ([with]); a picked video variant leaves the maximum height as it was.
 */
internal data class MpvTrackRequest(
    val disabled: Set<MpvTrackKind> = emptySet(),
    val picked: Map<MpvTrackKind, Int> = emptyMap(),
    val maxVideoHeight: Int = Int.MAX_VALUE,
    val pickedVideoVariant: Int? = null,
) {
    /**
     * What plays from the set of variants numbered [programId] among [tracks]: the variant picked only in the set of
     * the picked video track.
     */
    fun variantPick(programId: Int, tracks: List<MpvTrack>): HlsVariantPick {
        val pickedTrack = picked[MpvTrackKind.Video]?.let { id -> tracks.track(MpvTrackKind.Video, id) }
        return when {
            pickedVideoVariant != null && pickedTrack != null && programId in pickedTrack.programIds ->
                HlsVariantPick.Fixed(pickedVideoVariant)
            maxVideoHeight != Int.MAX_VALUE -> HlsVariantPick.MaxHeight(maxVideoHeight)
            else -> HlsVariantPick.Auto
        }
    }

    /** Groups and tracks are named by their positions in [tracks], which the player lists as [mpvVideoTracks]. */
    fun with(selection: VideoTrackSelection, tracks: VideoTracks): MpvTrackRequest {
        val cleared = copy(picked = picked - MpvTrackKind.Video, pickedVideoVariant = null)
        return when (selection) {
            VideoTrackSelection.Auto -> cleared.copy(maxVideoHeight = Int.MAX_VALUE)
            is VideoTrackSelection.MaxHeight -> cleared.copy(maxVideoHeight = selection.height)
            is VideoTrackSelection.Variant -> {
                val group = tracks.group(TrackKind.Video, selection.groupIndex, listOf(selection.trackIndex))
                val id = group?.let(::mpvTrackIdOf) ?: return cleared
                cleared.copy(
                    picked = cleared.picked + (MpvTrackKind.Video to id),
                    pickedVideoVariant = selection.trackIndex.takeIf { group.tracks.size > 1 },
                )
            }
        }
    }

    /** An audio group plays whole, so it is picked only when one of its tracks can play. */
    fun with(selection: AudioTrackSelection, tracks: VideoTracks): MpvTrackRequest {
        val cleared = copy(picked = picked - MpvTrackKind.Audio)
        if (selection !is AudioTrackSelection.Group) return cleared
        val supported = tracks.groups.getOrNull(selection.groupIndex)?.tracks
            ?.let { groupTracks -> groupTracks.indices.filter { groupTracks[it].supported } }
            .orEmpty()
        val id = tracks.group(TrackKind.Audio, selection.groupIndex, supported)?.let(::mpvTrackIdOf) ?: return cleared
        return cleared.copy(picked = cleared.picked + (MpvTrackKind.Audio to id))
    }

    /** A subtitle track that names nothing turns subtitles off, as [TextTrackSelection.Off] does. */
    fun with(selection: TextTrackSelection, tracks: VideoTracks): MpvTrackRequest {
        val track = selection as? TextTrackSelection.Track
        val id = track?.let { tracks.group(TrackKind.Text, it.groupIndex, listOf(it.trackIndex)) }?.let(::mpvTrackIdOf)
        val off = selection == TextTrackSelection.Off || (track != null && id == null)
        return copy(
            disabled = if (off) disabled + MpvTrackKind.Subtitle else disabled - MpvTrackKind.Subtitle,
            picked = picked - MpvTrackKind.Subtitle + listOfNotNull(id?.let { MpvTrackKind.Subtitle to it }),
        )
    }
}

/** The group [groupIndex] of [kind], when [trackIndices] name some of its tracks and nothing outside it. */
private fun VideoTracks.group(kind: TrackKind, groupIndex: Int, trackIndices: List<Int>): VideoTrackGroup? {
    val group = groups.getOrNull(groupIndex)?.takeIf { it.kind == kind } ?: return null
    if (trackIndices.isEmpty() || trackIndices.any { it !in group.tracks.indices }) return null
    return group
}

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

private const val AUTO = "auto"
private const val NONE = "no"
