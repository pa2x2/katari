package eu.kanade.tachiyomi.ui.video.player

import app.katari.entry.interactions.anime.stream.OFF_SUBTITLE_KEY
import app.katari.entry.interactions.anime.stream.automaticSubtitle
import app.katari.entry.interactions.anime.stream.sourceDefaultSubtitle
import app.katari.entry.interactions.anime.stream.subtitlePreferenceKey
import app.katari.video.engine.spi.tracks.subtitleChoiceKey
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.tracks.VideoPlayerAudioTrackOption
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.domain.entry.model.PlayerQualityMode

internal data class VideoPlaybackUiState(
    val sourceSelection: PlaybackSelection,
    val preferredSourceQualityKey: String?,
    val sessionPlaybackSpeed: Float,
    val currentStream: VideoStream,
    val currentStreamLabel: String,
    val streamOptions: List<VideoPlaybackOption>,
    val playbackData: PlaybackDescriptor,
    val preview: VideoPlaybackPreviewState = VideoPlaybackPreviewState(),
    val adaptiveQualities: List<VideoAdaptiveQualityOption> = emptyList(),
    val currentAdaptiveQuality: VideoAdaptiveQualityPreference = VideoAdaptiveQualityPreference.Auto,
    val subtitleOptions: List<VideoPlayerSubtitleOption> = emptyList(),
    val audioTracks: List<VideoPlayerAudioTrackOption> = emptyList(),
    /** The audio track picked this session; null leaves the choice to the stream. */
    val currentAudioTrackKey: String? = null,
    val currentSubtitle: VideoPlayerSubtitleSelection = VideoPlayerSubtitleSelection.None,
    val subtitleAppearance: VideoSubtitleAppearance = VideoSubtitleAppearance(),
    /** The title's engine; null leaves it to Auto. */
    val engineOverride: PlayerEngineOverride? = null,
    /** The title has had its one hint that the mpv add-on would play it better. */
    val mpvHintShown: Boolean = false,
    val pictureFraming: PictureFraming = PictureFraming.Fit,
    /** Plays a downloaded file, whose selection is the download's and isn't saved as the title's. */
    val downloaded: Boolean = false,
) {
    val subtitles: List<VideoSubtitle>
        get() = currentStream.subtitles

    val showsAdaptiveQualitySelector: Boolean
        get() = playbackData.sourceQualities.isEmpty() && adaptiveQualities.size > 1

    val hasSubtitleSelection: Boolean
        get() = subtitles.isNotEmpty() || subtitleOptions.any { it.selection is VideoPlayerSubtitleSelection.Embedded }

    val displayedPlaybackData: PlaybackDescriptor
        get() = preview.playbackData ?: playbackData

    val isPreviewLoading: Boolean
        get() = preview.isLoading

    val persistedSourceSelection: PlaybackSelection
        get() = sourceSelection.copy(
            sourceQualityKey = preferredSourceQualityKey ?: sourceSelection.sourceQualityKey,
        )
}

/**
 * A drafted dub's or server's options; [failed] when the source couldn't list them for [selection], [notOffered] when
 * it answered with another dub or server because it doesn't offer that one for the episode.
 */
internal data class VideoPlaybackPreviewState(
    val selection: PlaybackSelection? = null,
    val playbackData: PlaybackDescriptor? = null,
    val subtitles: List<VideoSubtitle>? = null,
    val isLoading: Boolean = false,
    val failed: Boolean = false,
    val notOffered: Boolean = false,
)

internal sealed interface VideoAdaptiveQualityPreference {
    data object Auto : VideoAdaptiveQualityPreference

    data class SpecificHeight(val height: Int) : VideoAdaptiveQualityPreference

    fun toPlayerQualityMode(): PlayerQualityMode {
        return when (this) {
            Auto -> PlayerQualityMode.AUTO
            is SpecificHeight -> PlayerQualityMode.SPECIFIC_HEIGHT
        }
    }

    fun heightOrNull(): Int? {
        return when (this) {
            Auto -> null
            is SpecificHeight -> height
        }
    }
}

internal data class VideoAdaptiveQualityOption(
    val label: String,
    val preference: VideoAdaptiveQualityPreference,
)

internal data class VideoPlaybackSpeedOption(
    val speed: Float,
    val label: String,
)

/** [needsMpv] marks a subtitle the playing engine can't decode, which only mpv can show. */
internal data class VideoPlayerSubtitleOption(
    val key: String,
    val label: String,
    val selection: VideoPlayerSubtitleSelection,
    val needsMpv: Boolean = false,
)

internal sealed interface VideoPlayerSubtitleSelection {
    data object None : VideoPlayerSubtitleSelection

    data object Default : VideoPlayerSubtitleSelection

    data class External(val subtitle: VideoSubtitle) : VideoPlayerSubtitleSelection

    data class Embedded(
        val groupIndex: Int,
        val trackIndex: Int,
        val key: String,
        val label: String,
        val language: String?,
        val isDefault: Boolean,
        val isForced: Boolean,
    ) : VideoPlayerSubtitleSelection
}

internal fun PlaybackSelection.withSelectedStream(streamKey: String?): PlaybackSelection {
    return copy(streamKey = streamKey)
}

internal fun PlaybackSelection.withSelectedDub(
    dubKey: String?,
    originalSelection: PlaybackSelection,
): PlaybackSelection {
    return copy(
        dubKey = dubKey,
        streamKey = null,
    ).restoringOriginalStreamIfCompatible(originalSelection)
}

internal fun PlaybackSelection.withSelectedServer(
    serverKey: String?,
    originalSelection: PlaybackSelection,
): PlaybackSelection {
    return copy(
        serverKey = serverKey,
        streamKey = null,
    ).restoringOriginalStreamIfCompatible(originalSelection)
}

internal fun PlaybackSelection.withSelectedSourceQuality(
    sourceQualityKey: String?,
    originalSelection: PlaybackSelection,
): PlaybackSelection {
    return copy(
        sourceQualityKey = sourceQualityKey,
        streamKey = null,
    ).restoringOriginalStreamIfCompatible(originalSelection)
}

private fun PlaybackSelection.restoringOriginalStreamIfCompatible(
    originalSelection: PlaybackSelection,
): PlaybackSelection {
    return if (
        dubKey == originalSelection.dubKey &&
        serverKey == originalSelection.serverKey &&
        sourceQualityKey == originalSelection.sourceQualityKey
    ) {
        copy(streamKey = originalSelection.streamKey)
    } else {
        this
    }
}

internal fun defaultSubtitleSelection(subtitles: List<VideoSubtitle>): VideoPlayerSubtitleSelection {
    return sourceDefaultSubtitle(subtitles).toSubtitleSelection()
}

internal fun resolveSourceSubtitleSelection(
    requested: VideoPlayerSubtitleSelection?,
    subtitles: List<VideoSubtitle>,
): VideoPlayerSubtitleSelection {
    return when (requested) {
        null -> defaultSubtitleSelection(subtitles)
        VideoPlayerSubtitleSelection.None -> VideoPlayerSubtitleSelection.None
        VideoPlayerSubtitleSelection.Default -> defaultSubtitleSelection(subtitles)
        is VideoPlayerSubtitleSelection.External -> {
            (
                subtitles.firstOrNull { subtitlePreferenceKey(it) == subtitlePreferenceKey(requested.subtitle) }
                    ?: subtitles.firstOrNull { subtitleChoiceKey(it) == subtitleChoiceKey(requested.subtitle) }
                )
                ?.let(VideoPlayerSubtitleSelection::External)
                ?: defaultSubtitleSelection(subtitles)
        }
        is VideoPlayerSubtitleSelection.Embedded -> requested
    }
}

internal fun resolvePersistedSubtitleSelection(
    subtitleKey: String?,
    subtitles: List<VideoSubtitle>,
): VideoPlayerSubtitleSelection {
    return automaticSubtitle(subtitleKey, subtitles).toSubtitleSelection()
}

private fun VideoSubtitle?.toSubtitleSelection(): VideoPlayerSubtitleSelection =
    this?.let(VideoPlayerSubtitleSelection::External) ?: VideoPlayerSubtitleSelection.None

internal fun subtitlePreferenceKey(selection: VideoPlayerSubtitleSelection): String? {
    return when (selection) {
        VideoPlayerSubtitleSelection.None -> OFF_SUBTITLE_KEY
        VideoPlayerSubtitleSelection.Default -> null
        is VideoPlayerSubtitleSelection.External -> subtitlePreferenceKey(selection.subtitle)
        is VideoPlayerSubtitleSelection.Embedded -> null
    }
}

internal fun resolvePreviewSubtitleSelection(
    requested: VideoPlayerSubtitleSelection,
    subtitles: List<VideoSubtitle>,
): VideoPlayerSubtitleSelection {
    return when (requested) {
        is VideoPlayerSubtitleSelection.Embedded -> defaultSubtitleSelection(subtitles)
        else -> resolveSourceSubtitleSelection(requested, subtitles)
    }
}

internal fun externalSubtitleOptions(subtitles: List<VideoSubtitle>): List<VideoPlayerSubtitleOption> {
    return buildList {
        add(
            VideoPlayerSubtitleOption(
                key = OFF_SUBTITLE_KEY,
                label = "Off",
                selection = VideoPlayerSubtitleSelection.None,
            ),
        )
        subtitles.forEach { subtitle ->
            add(
                VideoPlayerSubtitleOption(
                    key = subtitleChoiceKey(subtitle),
                    label = subtitle.label.ifBlank {
                        subtitle.language?.takeIf(String::isNotBlank) ?: "Subtitle"
                    },
                    selection = VideoPlayerSubtitleSelection.External(subtitle),
                ),
            )
        }
    }.distinctBy(VideoPlayerSubtitleOption::key)
}

internal fun subtitleSelectionKey(selection: VideoPlayerSubtitleSelection): String {
    return when (selection) {
        VideoPlayerSubtitleSelection.None -> OFF_SUBTITLE_KEY
        VideoPlayerSubtitleSelection.Default -> OFF_SUBTITLE_KEY
        is VideoPlayerSubtitleSelection.External -> subtitleChoiceKey(selection.subtitle)
        is VideoPlayerSubtitleSelection.Embedded -> selection.key
    }
}

internal val videoPlaybackSpeedOptions = listOf(
    VideoPlaybackSpeedOption(speed = 0.5f, label = "0.5x"),
    VideoPlaybackSpeedOption(speed = 0.75f, label = "0.75x"),
    VideoPlaybackSpeedOption(speed = 1f, label = "1x"),
    VideoPlaybackSpeedOption(speed = 1.25f, label = "1.25x"),
    VideoPlaybackSpeedOption(speed = 1.5f, label = "1.5x"),
    VideoPlaybackSpeedOption(speed = 2f, label = "2x"),
)

/** The offered speed after [speed], or before it, staying at the last or first; a speed between two goes to either. */
internal fun steppedPlaybackSpeed(speed: Float, faster: Boolean): Float {
    val speeds = videoPlaybackSpeedOptions.map { it.speed }
    return if (faster) {
        speeds.firstOrNull { it > speed } ?: speeds.last()
    } else {
        speeds.lastOrNull { it < speed } ?: speeds.first()
    }
}

internal fun videoPlaybackSpeedLabel(speed: Float): String =
    videoPlaybackSpeedOptions.firstOrNull { it.speed == speed }?.label ?: "${speed}x"

internal fun PlaybackPreferences.toAdaptiveQualityPreference(): VideoAdaptiveQualityPreference {
    return when (playerQualityMode) {
        PlayerQualityMode.AUTO -> VideoAdaptiveQualityPreference.Auto
        PlayerQualityMode.SPECIFIC_HEIGHT ->
            playerQualityHeight
                ?.let(VideoAdaptiveQualityPreference::SpecificHeight)
                ?: VideoAdaptiveQualityPreference.Auto
    }
}

internal const val DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED = 1f
