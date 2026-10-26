package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.ui.video.player.VideoAdaptiveQualityPreference
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackUiState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.defaultSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.externalSubtitleOptions
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoSourceChoice
import eu.kanade.tachiyomi.ui.video.player.resolvePreviewSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.subtitleSelectionKey
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedOptions
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RestartAlt
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A section the settings can open at: from the row under the timeline, a notice or a problem screen. */
internal enum class VideoPlayerSettingsSection { Dub, Server, Stream, SourceQuality, Speed, Subtitles, Chapters }

internal val VideoSourceChoice.settingsSection: VideoPlayerSettingsSection
    get() = when (this) {
        VideoSourceChoice.Server -> VideoPlayerSettingsSection.Server
        VideoSourceChoice.Dub -> VideoPlayerSettingsSection.Dub
        VideoSourceChoice.Quality -> VideoPlayerSettingsSection.SourceQuality
        VideoSourceChoice.Stream -> VideoPlayerSettingsSection.Stream
    }

/**
 * Speed, picture, playback quality and subtitles change as soon as they're tapped. Dub, server, stream and source
 * quality each reload the stream, so they collect into a source draft that Apply commits in one reload; while a
 * different dub or server is drafted, the options offered are its own and a subtitle pick joins the draft.
 */
@Composable
internal fun VideoPlayerSettingsContent(
    frame: VideoPlayerSettingsFrame,
    playback: VideoPlaybackUiState,
    initialSection: VideoPlayerSettingsSection?,
    onDismissRequest: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectPicture: (PictureFraming) -> Unit,
    onSelectAdaptiveQuality: (VideoAdaptiveQualityPreference) -> Unit,
    onSelectSubtitle: (VideoPlayerSubtitleSelection) -> Unit,
    onSelectAudioTrack: (String) -> Unit,
    onApplySource: (PlaybackSelection, VideoPlayerSubtitleSelection?) -> Unit,
    onReset: () -> Unit,
    onPreviewSourceSelection: (PlaybackSelection) -> Unit,
    onOpenSubtitleSettings: () -> Unit,
    engine: VideoPlayerEngineChoice,
    onSelectEngine: (PlayerEngineOverride?) -> Unit,
    chapters: List<VideoPlayerChapter>,
    currentChapter: VideoPlayerChapter?,
    onSelectChapter: (VideoPlayerChapter) -> Unit,
    /** The title's skip choice and where its times come from; null when nothing in the episode is marked to skip. */
    markerSkip: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val activeSource = playback.persistedSourceSelection
    val sourceOptions = VideoSourceOptions(
        listed = playback.playbackData,
        current = playback.sourceSelection,
        active = activeSource,
        preview = playback.preview,
    )
    var sourceDraft by remember(activeSource) { mutableStateOf(activeSource) }
    // Null until a subtitle is picked for a drafted dub; until then the current choice carries over where it can.
    var draftedSubtitle by remember(activeSource) { mutableStateOf<VideoPlayerSubtitleSelection?>(null) }
    val sourceRows = videoSourceRows(sourceOptions, sourceDraft)
    val draftMatchesPlaying = sourceRows.matchesCurrent
    val previewSubtitles = playback.preview.subtitles.orEmpty()
    val subtitleOptions = if (draftMatchesPlaying) {
        val mpvOnlyKeys = playback.subtitleOptions.filter { it.needsMpv }.mapTo(HashSet()) { it.key }
        (
            externalSubtitleOptions(playback.subtitles) + playback.subtitleOptions.filter {
                it.selection is VideoPlayerSubtitleSelection.Embedded
            }
            ).distinctBy { it.key }
            .map { it.copy(needsMpv = it.key in mpvOnlyKeys) }
    } else {
        externalSubtitleOptions(previewSubtitles)
    }
    val subtitleOptionsLoading = sourceRows.previewLoading && playback.preview.subtitles == null
    val hasSelectableSubtitleOptions = subtitleOptions.any { it.selection != VideoPlayerSubtitleSelection.None }
    val selectedSubtitle = if (draftMatchesPlaying) {
        playback.currentSubtitle
    } else {
        draftedSubtitle ?: resolvePreviewSubtitleSelection(playback.currentSubtitle, previewSubtitles)
    }
    val showSubtitles = subtitleOptionsLoading || hasSelectableSubtitleOptions
    // The tracks are the playing stream's, so they don't apply to a drafted dub.
    val showAudioTracks = draftMatchesPlaying && playback.audioTracks.size > 1
    // Mirrors the order of the items below, so a section can be scrolled to by index.
    val sectionKeys = buildList {
        if (sourceRows.showDubs) add(VideoSourceRowKeys.DUB)
        if (sourceRows.showServers) add(VideoSourceRowKeys.SERVER)
        if (sourceRows.showStreams) add(VideoSourceRowKeys.STREAM)
        if (sourceRows.showQualities) add(VideoSourceRowKeys.SOURCE_QUALITY)
        if (sourceRows.changed) add(VideoSourceRowKeys.APPLY)
        if (playback.showsAdaptiveQualitySelector) add(QUALITY_ITEM)
        add(SPEED_ITEM)
        add(PICTURE_ITEM)
        if (showAudioTracks) add(AUDIO_ITEM)
        if (showSubtitles) add(SUBTITLES_ITEM)
        if (chapters.isNotEmpty()) addAll(videoPlayerChapterItemKeys(chapters))
        if (markerSkip != null) add(MARKER_SKIP_ITEM)
        add(ENGINE_ITEM)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(initialSection) {
        val key = when (initialSection) {
            VideoPlayerSettingsSection.Dub -> VideoSourceRowKeys.DUB
            VideoPlayerSettingsSection.Server -> VideoSourceRowKeys.SERVER
            VideoPlayerSettingsSection.Stream -> VideoSourceRowKeys.STREAM
            VideoPlayerSettingsSection.SourceQuality -> VideoSourceRowKeys.SOURCE_QUALITY
            VideoPlayerSettingsSection.Speed -> SPEED_ITEM
            VideoPlayerSettingsSection.Subtitles -> SUBTITLES_ITEM
            VideoPlayerSettingsSection.Chapters -> CHAPTERS_ITEM
            null -> return@LaunchedEffect
        }
        sectionKeys.indexOf(key).takeIf { it >= 0 }?.let { listState.scrollToItem(it) }
    }

    VideoSourcePreviewEffect(sourceOptions, sourceDraft, onPreviewSourceSelection)

    val resetMenu: @Composable ColumnScope.(close: () -> Unit) -> Unit = { close ->
        DropdownMenuItem(
            label = stringResource(MR.strings.action_reset),
            onClick = {
                close()
                onReset()
                onDismissRequest()
            },
            icon = MaterialSymbols.Rounded.RestartAlt,
        )
    }

    Column(modifier = modifier) {
        VideoPlayerSettingsHeader(frame = frame, onDismissRequest = onDismissRequest, menu = resetMenu)
        LazyColumn(
            // A sheet shrinks to its rows; the full-height panel keeps them from the top.
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = frame == VideoPlayerSettingsFrame.Panel),
            state = listState,
            contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
        ) {
            videoSourceRowItems(
                rows = sourceRows,
                draft = sourceDraft,
                options = sourceOptions,
                onDraft = { draft ->
                    if (draft.dubKey != sourceDraft.dubKey || draft.serverKey != sourceDraft.serverKey) {
                        draftedSubtitle = null
                    }
                    sourceDraft = draft
                },
                onRetryPreview = { onPreviewSourceSelection(sourceDraft) },
                onCancel = {
                    sourceDraft = activeSource
                    draftedSubtitle = null
                },
                onApply = {
                    // An embedded track belongs to the stream being replaced.
                    val subtitle = when {
                        draftMatchesPlaying -> null
                        selectedSubtitle is VideoPlayerSubtitleSelection.Embedded ->
                            defaultSubtitleSelection(previewSubtitles)
                        else -> selectedSubtitle
                    }
                    onApplySource(sourceDraft, subtitle)
                    onDismissRequest()
                },
            )

            if (playback.showsAdaptiveQualitySelector) {
                item(key = QUALITY_ITEM) {
                    ChipsRow(title = stringResource(MR.strings.anime_playback_quality)) {
                        playback.adaptiveQualities.forEach { option ->
                            FilterChip(
                                label = option.label,
                                selected = option.preference == playback.currentAdaptiveQuality,
                                onClick = { onSelectAdaptiveQuality(option.preference) },
                            )
                        }
                    }
                }
            }

            item(key = SPEED_ITEM) {
                ChipsRow(title = stringResource(MR.strings.anime_playback_speed)) {
                    videoPlaybackSpeedOptions.forEach { option ->
                        FilterChip(
                            label = option.label,
                            selected = option.speed == playback.sessionPlaybackSpeed,
                            onClick = { onSelectSpeed(option.speed) },
                        )
                    }
                }
            }

            item(key = PICTURE_ITEM) {
                VideoPlayerPictureRow(framing = playback.pictureFraming, onSelect = onSelectPicture)
            }

            if (showAudioTracks) {
                item(key = AUDIO_ITEM) {
                    AudioTrackRow(
                        tracks = playback.audioTracks,
                        currentKey = playback.currentAudioTrackKey,
                        trackPick = engine.trackPick,
                        onSelect = onSelectAudioTrack,
                    )
                }
            }

            if (showSubtitles) {
                item(key = SUBTITLES_ITEM) {
                    if (subtitleOptionsLoading) {
                        LoadingPlaybackOptionRow(titleRes = MR.strings.anime_playback_subtitles)
                    } else {
                        SubtitlePlaybackOptionRow(
                            options = subtitleOptions,
                            selectedKey = subtitleSelectionKey(selectedSubtitle),
                            trackPick = engine.trackPick,
                            onSelect = { key ->
                                val selection = subtitleOptions.firstOrNull { it.key == key }?.selection
                                    ?: return@SubtitlePlaybackOptionRow
                                if (draftMatchesPlaying) {
                                    onSelectSubtitle(selection)
                                } else {
                                    draftedSubtitle = selection
                                }
                            },
                            onOpenSubtitleSettings = onOpenSubtitleSettings.takeIf { hasSelectableSubtitleOptions },
                        )
                    }
                }
            }

            if (chapters.isNotEmpty()) {
                videoPlayerChapterItems(chapters = chapters, current = currentChapter, onSelect = onSelectChapter)
            }

            if (markerSkip != null) {
                item(key = MARKER_SKIP_ITEM) {
                    Column { markerSkip() }
                }
            }

            item(key = ENGINE_ITEM) {
                VideoPlayerEngineRow(choice = engine, onSelect = onSelectEngine)
            }
        }
    }
}

private const val QUALITY_ITEM = "quality"
private const val SPEED_ITEM = "speed"
private const val PICTURE_ITEM = "picture"
private const val AUDIO_ITEM = "audio"
private const val SUBTITLES_ITEM = "subtitles"
private const val MARKER_SKIP_ITEM = "marker_skip"
private const val ENGINE_ITEM = "engine"
