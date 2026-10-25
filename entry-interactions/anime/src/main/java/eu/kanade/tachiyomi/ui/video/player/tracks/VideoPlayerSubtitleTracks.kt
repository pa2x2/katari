package eu.kanade.tachiyomi.ui.video.player.tracks

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleOption
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.externalSubtitleOptions
import mihon.video.engine.spi.tracks.SelfDrawnSubtitles
import mihon.video.engine.spi.tracks.subtitleChoiceKey
import java.util.Locale

internal fun Player.availableSubtitleTracks(
    externalSubtitles: List<VideoSubtitle>,
): List<VideoPlayerSubtitleOption> {
    val externalSubtitleKeys = externalSubtitles.mapTo(mutableSetOf(), ::subtitleChoiceKey)
    val externalSubtitleFingerprints = externalSubtitles.mapTo(mutableSetOf(), ::subtitleTrackFingerprint)
    // A subtitle in a format the player doesn't know has no track type, so it is found by its id in any group.
    val unplayable = currentTracks.groups
        .flatMap { group -> (0 until group.length).filterNot(group::isTrackSupported).map(group::getTrackFormat) }
    val options = externalSubtitleOptions(externalSubtitles).mapTo(mutableListOf()) { option ->
        val subtitle = (option.selection as? VideoPlayerSubtitleSelection.External)?.subtitle
            ?: return@mapTo option
        val key = subtitleChoiceKey(subtitle)
        option.copy(needsMpv = unplayable.any { it.isExternalSubtitle(key) })
    }

    currentTracks.groups.forEachIndexed { groupIndex, group ->
        if (group.getType() != C.TRACK_TYPE_TEXT) return@forEachIndexed
        repeat(group.length) { trackIndex ->
            val format = group.getTrackFormat(trackIndex)
            if (
                externalSubtitleKeys.any(format::isExternalSubtitle) ||
                format.subtitleTrackFingerprint() in externalSubtitleFingerprints ||
                !shouldExposeEmbeddedSubtitle(format)
            ) {
                return@repeat
            }
            val subtitleSelection = embeddedSubtitleSelection(groupIndex, trackIndex, group)
            options += VideoPlayerSubtitleOption(
                key = subtitleSelection.key,
                label = subtitleSelection.label,
                selection = subtitleSelection,
                needsMpv = !group.isTrackSupported(trackIndex),
            )
        }
    }

    return options.distinctBy(VideoPlayerSubtitleOption::key)
}

internal fun Player.resolveAppliedSubtitleSelection(
    requested: VideoPlayerSubtitleSelection,
    externalSubtitles: List<VideoSubtitle>,
): VideoPlayerSubtitleSelection {
    return when (requested) {
        VideoPlayerSubtitleSelection.None -> requested
        VideoPlayerSubtitleSelection.Default -> {
            if (externalSubtitles.isEmpty()) {
                currentTracks.groups
                    .firstNotNullOfOrNull { group ->
                        if (group.getType() != C.TRACK_TYPE_TEXT || !group.isSupported()) {
                            return@firstNotNullOfOrNull null
                        }
                        (0 until group.length)
                            .firstOrNull { index ->
                                group.isTrackSupported(index) &&
                                    group.getTrackFormat(index).selectionFlags and C.SELECTION_FLAG_DEFAULT != 0
                            }
                            ?.let { index -> group to index }
                    }
                    ?.let { (group, index) ->
                        currentTracks.groups.indexOf(group).takeIf { it >= 0 }?.let { groupIndex ->
                            embeddedSubtitleSelection(groupIndex, index, group)
                        }
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
            val selectedExternalSubtitle = externalSubtitles.firstOrNull {
                subtitleChoiceKey(it) == subtitleChoiceKey(requested.subtitle)
            } ?: return VideoPlayerSubtitleSelection.None
            if (matchingTextTrack(selectedExternalSubtitle) != null) {
                VideoPlayerSubtitleSelection.External(selectedExternalSubtitle)
            } else {
                VideoPlayerSubtitleSelection.External(selectedExternalSubtitle)
            }
        }
        is VideoPlayerSubtitleSelection.Embedded -> {
            matchingEmbeddedTextTrack(requested)
                ?.let { match -> embeddedSubtitleSelection(match.groupIndex, match.trackIndex, match.group) }
                ?: requested
        }
    }
}

internal fun Player.applySubtitleSelection(selection: VideoPlayerSubtitleSelection) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    when (selection) {
        VideoPlayerSubtitleSelection.None -> {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            builder.setPreferredTextLanguage(null)
        }
        VideoPlayerSubtitleSelection.Default,
        -> {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            builder.setPreferredTextLanguage(null)
        }
        is VideoPlayerSubtitleSelection.External -> {
            val override = matchingTextTrack(selection.subtitle)
            if (override == null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                builder.setPreferredTextLanguage(null)
            } else {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.setPreferredTextLanguage(null)
                builder.setOverrideForType(override)
            }
        }
        is VideoPlayerSubtitleSelection.Embedded -> {
            val match = matchingEmbeddedTextTrack(selection)
            if (match == null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                builder.setPreferredTextLanguage(null)
            } else {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.setPreferredTextLanguage(null)
                builder.setOverrideForType(TrackSelectionOverride(match.group.getMediaTrackGroup(), match.trackIndex))
            }
        }
    }

    trackSelectionParameters = builder.build()
}

/**
 * Whether the selected subtitle keeps its own look, which the viewer's subtitle appearance doesn't reach: one the
 * player draws itself, or one made of pictures (PGS, VobSub, DVB), whose cues are bitmaps.
 */
internal fun Player.subtitleKeepsOwnStyle(): Boolean =
    (this as? SelfDrawnSubtitles)?.drawsSelectedSubtitle == true ||
        currentTracks.groups.any { group ->
            group.getType() == C.TRACK_TYPE_TEXT &&
                (0 until group.length).any { group.isTrackSelected(it) && group.getTrackFormat(it).isBitmapSubtitle() }
        }

// Subtitles parsed while extracting report Media3's cue format, with the original format in codecs.
private fun Format.isBitmapSubtitle(): Boolean =
    sampleMimeType in BITMAP_SUBTITLE_TYPES || codecs in BITMAP_SUBTITLE_TYPES

private val BITMAP_SUBTITLE_TYPES =
    setOf(MimeTypes.APPLICATION_PGS, MimeTypes.APPLICATION_VOBSUB, MimeTypes.APPLICATION_DVBSUBS)

private fun legacyEmbeddedSubtitleChoiceKey(groupIndex: Int, trackIndex: Int): String {
    return "embedded:$groupIndex:$trackIndex"
}

private fun embeddedSubtitleChoiceKey(format: Format): String {
    return "embedded:${format.subtitleTrackFingerprint()}"
}

private fun Player.matchingEmbeddedTextTrack(
    selection: VideoPlayerSubtitleSelection.Embedded,
): EmbeddedTextTrackMatch? {
    return currentTracks.groups.withIndex()
        .asSequence()
        .filter { (_, group) -> group.getType() == C.TRACK_TYPE_TEXT && group.isSupported() }
        .mapNotNull { (groupIndex, group) ->
            (0 until group.length)
                .firstOrNull { trackIndex ->
                    if (!group.isTrackSupported(trackIndex)) {
                        return@firstOrNull false
                    }
                    val format = group.getTrackFormat(trackIndex)
                    embeddedSubtitleChoiceKey(format) == selection.key ||
                        legacyEmbeddedSubtitleChoiceKey(groupIndex, trackIndex) == selection.key
                }
                ?.let { trackIndex -> EmbeddedTextTrackMatch(groupIndex, group, trackIndex) }
        }
        .firstOrNull()
}

private fun Player.matchingTextTrack(subtitle: VideoSubtitle): TrackSelectionOverride? {
    val subtitleKey = subtitleChoiceKey(subtitle)
    return currentTracks.groups.asSequence()
        .filter { it.getType() == C.TRACK_TYPE_TEXT && it.isSupported() }
        .mapNotNull { group ->
            (0 until group.length)
                .firstOrNull { trackIndex ->
                    if (!group.isTrackSupported(trackIndex)) {
                        return@firstOrNull false
                    }
                    group.getTrackFormat(trackIndex).isExternalSubtitle(subtitleKey)
                }
                ?.let { trackIndex -> TrackSelectionOverride(group.getMediaTrackGroup(), trackIndex) }
        }
        .firstOrNull()
}

// An engine numbers an external subtitle's id by its merged position, a second time when external audio is merged too.
private fun Format.isExternalSubtitle(key: String): Boolean = id == key || id?.endsWith(":$key") == true

private fun shouldExposeEmbeddedSubtitle(format: Format): Boolean {
    val label = format.label?.trim().orEmpty()
    val language = format.language?.trim().orEmpty()
    val hasMeaningfulLabel = label.isNotBlank() && !label.equals("subtitle", ignoreCase = true)
    val hasMeaningfulLanguage = language.isNotBlank() && !language.equals("und", ignoreCase = true)
    val hasFlags = format.selectionFlags and (C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED) != 0
    return hasMeaningfulLabel || hasMeaningfulLanguage || hasFlags
}

private fun embeddedSubtitleSelection(
    groupIndex: Int,
    trackIndex: Int,
    group: Tracks.Group,
): VideoPlayerSubtitleSelection.Embedded {
    val format = group.getTrackFormat(trackIndex)
    return VideoPlayerSubtitleSelection.Embedded(
        groupIndex = groupIndex,
        trackIndex = trackIndex,
        key = embeddedSubtitleChoiceKey(format),
        label = buildEmbeddedSubtitleLabel(
            label = format.label,
            language = format.language,
            isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0,
            isForced = format.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
        ),
        language = format.language,
        isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0,
        isForced = format.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
    )
}

private data class EmbeddedTextTrackMatch(
    val groupIndex: Int,
    val group: Tracks.Group,
    val trackIndex: Int,
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

private fun Format.subtitleTrackFingerprint(): String {
    return listOf(
        label?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        language?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        (selectionFlags and C.SELECTION_FLAG_DEFAULT != 0).toString(),
        (selectionFlags and C.SELECTION_FLAG_FORCED != 0).toString(),
    ).joinToString("|")
}
