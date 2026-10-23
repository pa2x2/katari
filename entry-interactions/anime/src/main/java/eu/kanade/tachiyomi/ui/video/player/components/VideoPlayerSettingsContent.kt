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
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.ui.video.player.VideoAdaptiveQualityPreference
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackUiState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.defaultSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.externalSubtitleOptions
import eu.kanade.tachiyomi.ui.video.player.resolvePreviewSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.subtitleSelectionKey
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedOptions
import eu.kanade.tachiyomi.ui.video.player.withSelectedDub
import eu.kanade.tachiyomi.ui.video.player.withSelectedSourceQuality
import eu.kanade.tachiyomi.ui.video.player.withSelectedStream
import mihon.entry.viewer.chrome.ui.OverlayDrawerHeader
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RestartAlt
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A section the row under the timeline can open the settings at. */
internal enum class VideoPlayerSettingsSection { Speed, Subtitles }

/**
 * Speed, playback quality and subtitles change as soon as they're tapped. Dub, stream and source quality each reload
 * the stream, so they collect into a source draft that "Apply source" commits in one reload; while a different dub is
 * drafted, the subtitles offered are that dub's and the pick joins the draft.
 */
@Composable
internal fun VideoPlayerSettingsContent(
    frame: VideoPlayerSettingsFrame,
    playback: VideoPlaybackUiState,
    initialSection: VideoPlayerSettingsSection?,
    onDismissRequest: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectAdaptiveQuality: (VideoAdaptiveQualityPreference) -> Unit,
    onSelectSubtitle: (VideoPlayerSubtitleSelection) -> Unit,
    onSelectAudioTrack: (String) -> Unit,
    onApplySource: (PlaybackSelection, VideoPlayerSubtitleSelection?) -> Unit,
    onReset: () -> Unit,
    onPreviewSourceSelection: (PlaybackSelection) -> Unit,
    onOpenSubtitleSettings: () -> Unit,
    engine: VideoPlayerEngineChoice,
    onSelectEngine: (PlayerEngineOverride?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSource = playback.persistedSourceSelection
    var sourceDraft by remember(activeSource) { mutableStateOf(activeSource) }
    // Null until a subtitle is picked for a drafted dub; until then the current choice carries over where it can.
    var draftedSubtitle by remember(activeSource) { mutableStateOf<VideoPlayerSubtitleSelection?>(null) }
    val draftDubMatchesActive = sourceDraft.dubKey == playback.sourceSelection.dubKey
    val sourceChanged = sourceDraft != activeSource
    val sourceQualityOptions = if (draftDubMatchesActive) {
        playback.playbackData.sourceQualities
    } else {
        playback.preview.playbackData?.sourceQualities.orEmpty()
    }
    val previewLoading = !draftDubMatchesActive &&
        playback.isPreviewLoading &&
        playback.preview.selection == sourceDraft
    val qualityOptionsLoading = previewLoading && sourceQualityOptions.isEmpty()
    val previewSubtitles = playback.preview.subtitles.orEmpty()
    val subtitleOptions = if (draftDubMatchesActive) {
        (
            externalSubtitleOptions(playback.subtitles) + playback.subtitleOptions.filter {
                it.selection is VideoPlayerSubtitleSelection.Embedded
            }
            ).distinctBy { it.key }
    } else {
        externalSubtitleOptions(previewSubtitles)
    }
    val subtitleOptionsLoading = previewLoading && playback.preview.subtitles == null
    val hasSelectableSubtitleOptions = subtitleOptions.any { it.selection != VideoPlayerSubtitleSelection.None }
    val selectedSubtitle = if (draftDubMatchesActive) {
        playback.currentSubtitle
    } else {
        draftedSubtitle ?: resolvePreviewSubtitleSelection(playback.currentSubtitle, previewSubtitles)
    }
    val streamOptionsEnabled = draftDubMatchesActive &&
        sourceDraft.sourceQualityKey == playback.sourceSelection.sourceQualityKey
    val showDubs = playback.playbackData.dubs.isNotEmpty()
    val showStreams = playback.streamOptions.size > 1
    val showSourceQualities = qualityOptionsLoading || sourceQualityOptions.isNotEmpty()
    val showSubtitles = subtitleOptionsLoading || hasSelectableSubtitleOptions
    // The tracks are the playing stream's, so they don't apply to a drafted dub.
    val showAudioTracks = draftDubMatchesActive && playback.audioTracks.size > 1
    // Mirrors the order of the items below, so a section can be scrolled to by index.
    val sectionKeys = buildList {
        if (showDubs) add(DUB_ITEM)
        if (showStreams) add(STREAM_ITEM)
        if (showSourceQualities) add(SOURCE_QUALITY_ITEM)
        if (sourceChanged) add(APPLY_SOURCE_ITEM)
        if (playback.showsAdaptiveQualitySelector) add(QUALITY_ITEM)
        add(SPEED_ITEM)
        if (showAudioTracks) add(AUDIO_ITEM)
        if (showSubtitles) add(SUBTITLES_ITEM)
        add(ENGINE_ITEM)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(initialSection) {
        val key = when (initialSection) {
            VideoPlayerSettingsSection.Speed -> SPEED_ITEM
            VideoPlayerSettingsSection.Subtitles -> SUBTITLES_ITEM
            null -> return@LaunchedEffect
        }
        sectionKeys.indexOf(key).takeIf { it >= 0 }?.let { listState.scrollToItem(it) }
    }

    LaunchedEffect(sourceDraft.dubKey) {
        onPreviewSourceSelection(if (draftDubMatchesActive) playback.sourceSelection else sourceDraft)
    }

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
        when (frame) {
            VideoPlayerSettingsFrame.Sheet -> SheetHeader(
                title = stringResource(MR.strings.action_settings),
                menu = resetMenu,
            )
            VideoPlayerSettingsFrame.Panel -> OverlayDrawerHeader(
                title = stringResource(MR.strings.action_settings),
                icon = MaterialSymbols.Rounded.Settings,
                closeDescription = stringResource(MR.strings.action_close),
                onClose = onDismissRequest,
                menu = resetMenu,
            )
        }
        LazyColumn(
            // A sheet shrinks to its rows; the full-height panel keeps them from the top.
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = frame == VideoPlayerSettingsFrame.Panel),
            state = listState,
            contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
        ) {
            if (showDubs) {
                item(key = DUB_ITEM) {
                    PlaybackOptionRow(
                        options = playback.playbackData.dubs,
                        titleRes = MR.strings.anime_playback_dub,
                        selectedKey = sourceDraft.dubKey,
                        onSelect = {
                            sourceDraft = sourceDraft.withSelectedDub(it, activeSource)
                            draftedSubtitle = null
                        },
                    )
                }
            }

            if (showStreams) {
                item(key = STREAM_ITEM) {
                    PlaybackOptionRow(
                        options = playback.streamOptions,
                        titleRes = MR.strings.anime_playback_stream,
                        selectedKey = sourceDraft.streamKey,
                        enabled = streamOptionsEnabled,
                        onSelect = { sourceDraft = sourceDraft.withSelectedStream(it) },
                    )
                }
            }

            if (showSourceQualities) {
                item(key = SOURCE_QUALITY_ITEM) {
                    if (qualityOptionsLoading) {
                        LoadingPlaybackOptionRow(titleRes = MR.strings.anime_playback_source_quality)
                    } else {
                        PlaybackOptionRow(
                            options = sourceQualityOptions,
                            titleRes = MR.strings.anime_playback_source_quality,
                            selectedKey = sourceDraft.sourceQualityKey,
                            onSelect = { sourceDraft = sourceDraft.withSelectedSourceQuality(it, activeSource) },
                        )
                    }
                }
            }

            if (sourceChanged) {
                item(key = APPLY_SOURCE_ITEM) {
                    ApplySourceRow(
                        onCancel = {
                            sourceDraft = activeSource
                            draftedSubtitle = null
                        },
                        onApply = {
                            // An embedded track belongs to the stream being replaced.
                            val subtitle = when {
                                draftDubMatchesActive -> null
                                selectedSubtitle is VideoPlayerSubtitleSelection.Embedded ->
                                    defaultSubtitleSelection(previewSubtitles)
                                else -> selectedSubtitle
                            }
                            onApplySource(sourceDraft, subtitle)
                            onDismissRequest()
                        },
                    )
                }
            }

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

            if (showAudioTracks) {
                item(key = AUDIO_ITEM) {
                    AudioTrackRow(
                        tracks = playback.audioTracks,
                        currentKey = playback.currentAudioTrackKey,
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
                            options = subtitleOptions.map { VideoPlaybackOption(key = it.key, label = it.label) },
                            selectedKey = subtitleSelectionKey(selectedSubtitle),
                            onSelect = { key ->
                                val selection = subtitleOptions.firstOrNull { it.key == key }?.selection
                                    ?: return@SubtitlePlaybackOptionRow
                                if (draftDubMatchesActive) {
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

            item(key = ENGINE_ITEM) {
                VideoPlayerEngineRow(choice = engine, onSelect = onSelectEngine)
            }
        }
    }
}

private const val DUB_ITEM = "dub"
private const val STREAM_ITEM = "stream"
private const val SOURCE_QUALITY_ITEM = "source_quality"
private const val APPLY_SOURCE_ITEM = "apply_source"
private const val QUALITY_ITEM = "quality"
private const val SPEED_ITEM = "speed"
private const val AUDIO_ITEM = "audio"
private const val SUBTITLES_ITEM = "subtitles"
private const val ENGINE_ITEM = "engine"
