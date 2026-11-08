package eu.kanade.tachiyomi.ui.video.player.tracks

import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.player.VideoPlayerState
import app.katari.video.engine.spi.tracks.TextTrackSelection
import app.katari.video.engine.spi.tracks.TrackKind
import app.katari.video.engine.spi.tracks.VideoTrack
import app.katari.video.engine.spi.tracks.VideoTrackGroup
import app.katari.video.engine.spi.tracks.VideoTracks
import app.katari.video.engine.spi.tracks.subtitleChoiceKey
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleOption
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.externalSubtitleOptions
import java.util.Locale

internal fun VideoTracks.availableSubtitleTracks(
    externalSubtitles: List<VideoSubtitle>,
): List<VideoPlayerSubtitleOption> {
    val externalSubtitleKeys = externalSubtitles.mapTo(mutableSetOf(), ::subtitleChoiceKey)
    val externalSubtitleFingerprints = externalSubtitles.mapTo(mutableSetOf(), ::subtitleTrackFingerprint)
    // A subtitle in a format the player doesn't know has no track kind, so it is found by its id in any group.
    val unplayable = groups.flatMap { group -> group.tracks.filterNot(VideoTrack::supported) }
    val options = externalSubtitleOptions(externalSubtitles).mapTo(mutableListOf()) { option ->
        val subtitle = (option.selection as? VideoPlayerSubtitleSelection.External)?.subtitle
            ?: return@mapTo option
        val key = subtitleChoiceKey(subtitle)
        option.copy(needsMpv = unplayable.any { it.isExternalSubtitle(key) })
    }

    groups.forEachIndexed { groupIndex, group ->
        if (group.kind != TrackKind.Text) return@forEachIndexed
        group.tracks.forEachIndexed { trackIndex, track ->
            if (
                externalSubtitleKeys.any(track::isExternalSubtitle) ||
                track.subtitleTrackFingerprint() in externalSubtitleFingerprints ||
                !shouldExposeEmbeddedSubtitle(track)
            ) {
                return@forEachIndexed
            }
            val subtitleSelection = embeddedSubtitleSelection(groupIndex, trackIndex, track)
            options += VideoPlayerSubtitleOption(
                key = subtitleSelection.key,
                label = subtitleSelection.label,
                selection = subtitleSelection,
                needsMpv = !track.supported,
            )
        }
    }

    return options.distinctBy(VideoPlayerSubtitleOption::key)
}

internal fun VideoTracks.resolveAppliedSubtitleSelection(
    requested: VideoPlayerSubtitleSelection,
    externalSubtitles: List<VideoSubtitle>,
): VideoPlayerSubtitleSelection {
    return when (requested) {
        VideoPlayerSubtitleSelection.None -> requested
        VideoPlayerSubtitleSelection.Default -> {
            if (externalSubtitles.isEmpty()) {
                groups.withIndex()
                    .firstNotNullOfOrNull { (groupIndex, group) ->
                        if (group.kind != TrackKind.Text || !group.supported) return@firstNotNullOfOrNull null
                        group.tracks.withIndex()
                            .firstOrNull { (_, track) -> track.supported && track.isDefault }
                            ?.let { (trackIndex, track) -> embeddedSubtitleSelection(groupIndex, trackIndex, track) }
                    }
                    ?: VideoPlayerSubtitleSelection.None
            } else {
                val defaultSubtitle = externalSubtitles.firstOrNull(VideoSubtitle::isDefault)
                    ?: externalSubtitles.firstOrNull()
                defaultSubtitle?.let { subtitle ->
                    if (matchingTextTrack(subtitle) != null) {
                        VideoPlayerSubtitleSelection.External(subtitle)
                    } else {
                        VideoPlayerSubtitleSelection.Default
                    }
                } ?: VideoPlayerSubtitleSelection.None
            }
        }
        is VideoPlayerSubtitleSelection.External -> {
            externalSubtitles
                .firstOrNull { subtitleChoiceKey(it) == subtitleChoiceKey(requested.subtitle) }
                ?.let(VideoPlayerSubtitleSelection::External)
                ?: VideoPlayerSubtitleSelection.None
        }
        is VideoPlayerSubtitleSelection.Embedded -> {
            matchingEmbeddedTextTrack(requested)
                ?.let { match -> embeddedSubtitleSelection(match.groupIndex, match.trackIndex, match.track) }
                ?: requested
        }
    }
}

internal fun VideoPlayer.applySubtitleSelection(selection: VideoPlayerSubtitleSelection) {
    val tracks = state.value.tracks
    selectText(
        when (selection) {
            VideoPlayerSubtitleSelection.None -> TextTrackSelection.Off
            VideoPlayerSubtitleSelection.Default -> TextTrackSelection.Default
            is VideoPlayerSubtitleSelection.External -> tracks.matchingTextTrack(selection.subtitle)
                ?.let { TextTrackSelection.Track(it.groupIndex, it.trackIndex) }
                ?: TextTrackSelection.Off
            is VideoPlayerSubtitleSelection.Embedded -> tracks.matchingEmbeddedTextTrack(selection)
                ?.let { TextTrackSelection.Track(it.groupIndex, it.trackIndex) }
                ?: TextTrackSelection.Off
        },
    )
}

/**
 * Whether the selected subtitle keeps its own look, which the viewer's subtitle appearance doesn't reach: one the
 * player draws itself, or one made of pictures (PGS, VobSub, DVB), whose cues are bitmaps.
 */
internal fun VideoPlayerState.subtitleKeepsOwnStyle(): Boolean =
    drawsSelectedSubtitle ||
        tracks.groups.any { group ->
            group.kind == TrackKind.Text && group.tracks.any { it.selected && it.isBitmapSubtitle }
        }

private fun legacyEmbeddedSubtitleChoiceKey(groupIndex: Int, trackIndex: Int): String {
    return "embedded:$groupIndex:$trackIndex"
}

private fun embeddedSubtitleChoiceKey(track: VideoTrack): String {
    return "embedded:${track.subtitleTrackFingerprint()}"
}

private fun VideoTracks.matchingEmbeddedTextTrack(
    selection: VideoPlayerSubtitleSelection.Embedded,
): TextTrackMatch? {
    return supportedTextTracks().firstOrNull { match ->
        embeddedSubtitleChoiceKey(match.track) == selection.key ||
            legacyEmbeddedSubtitleChoiceKey(match.groupIndex, match.trackIndex) == selection.key
    }
}

private fun VideoTracks.matchingTextTrack(subtitle: VideoSubtitle): TextTrackMatch? {
    val subtitleKey = subtitleChoiceKey(subtitle)
    return supportedTextTracks().firstOrNull { it.track.isExternalSubtitle(subtitleKey) }
}

/** The playable text tracks, the first match in each group before any of the next group. */
private fun VideoTracks.supportedTextTracks(): Sequence<TextTrackMatch> =
    groups.asSequence()
        .withIndex()
        .filter { (_, group) -> group.kind == TrackKind.Text && group.supported }
        .flatMap { (groupIndex, group) ->
            group.tracks.asSequence().withIndex()
                .filter { (_, track) -> track.supported }
                .map { (trackIndex, track) -> TextTrackMatch(groupIndex, trackIndex, track) }
        }

// An engine numbers an external subtitle's id by its merged position, a second time when external audio is merged too.
private fun VideoTrack.isExternalSubtitle(key: String): Boolean = id == key || id?.endsWith(":$key") == true

private fun shouldExposeEmbeddedSubtitle(track: VideoTrack): Boolean {
    val label = track.label?.trim().orEmpty()
    val language = track.language?.trim().orEmpty()
    val hasMeaningfulLabel = label.isNotBlank() && !label.equals("subtitle", ignoreCase = true)
    val hasMeaningfulLanguage = language.isNotBlank() && !language.equals("und", ignoreCase = true)
    return hasMeaningfulLabel || hasMeaningfulLanguage || track.isDefault || track.isForced
}

private fun embeddedSubtitleSelection(
    groupIndex: Int,
    trackIndex: Int,
    track: VideoTrack,
): VideoPlayerSubtitleSelection.Embedded {
    return VideoPlayerSubtitleSelection.Embedded(
        groupIndex = groupIndex,
        trackIndex = trackIndex,
        key = embeddedSubtitleChoiceKey(track),
        label = buildEmbeddedSubtitleLabel(
            label = track.label,
            language = track.language,
            isDefault = track.isDefault,
            isForced = track.isForced,
        ),
        language = track.language,
        isDefault = track.isDefault,
        isForced = track.isForced,
    )
}

private data class TextTrackMatch(
    val groupIndex: Int,
    val trackIndex: Int,
    val track: VideoTrack,
)

private fun buildEmbeddedSubtitleLabel(
    label: String?,
    language: String?,
    isDefault: Boolean,
    isForced: Boolean,
): String {
    val base = label?.takeIf(String::isNotBlank)
        ?: language?.takeIf(String::isNotBlank)
        ?: "Subtitle"
    val suffixes = buildList {
        if (isDefault) add("Default")
        if (isForced) add("Forced")
    }
    return if (suffixes.isEmpty()) {
        base
    } else {
        "$base (${suffixes.joinToString()})"
    }
}

private fun subtitleTrackFingerprint(subtitle: VideoSubtitle): String {
    return listOf(
        subtitle.label.trim().lowercase(Locale.ROOT),
        subtitle.language?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        subtitle.isDefault.toString(),
        subtitle.isForced.toString(),
    ).joinToString("|")
}

private fun VideoTrack.subtitleTrackFingerprint(): String {
    return listOf(
        label?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        language?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        isDefault.toString(),
        isForced.toString(),
    ).joinToString("|")
}
