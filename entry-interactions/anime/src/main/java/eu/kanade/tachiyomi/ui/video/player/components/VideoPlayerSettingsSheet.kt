package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.ui.video.player.VideoAdaptiveQualityPreference
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackUiState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSettingsDraft
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.defaultSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.defaultVideoPlayerSettingsDraft
import eu.kanade.tachiyomi.ui.video.player.externalSubtitleOptions
import eu.kanade.tachiyomi.ui.video.player.resolvePreviewSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.sourceSelectionForDisplay
import eu.kanade.tachiyomi.ui.video.player.subtitleSelectionKey
import eu.kanade.tachiyomi.ui.video.player.toSettingsDraft
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedOptions
import eu.kanade.tachiyomi.ui.video.player.withSelectedDub
import eu.kanade.tachiyomi.ui.video.player.withSelectedSourceQuality
import eu.kanade.tachiyomi.ui.video.player.withSelectedStream
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerSettingsSheet(
    playback: VideoPlaybackUiState,
    onDismissRequest: () -> Unit,
    onApplySettings: (VideoPlayerSettingsDraft) -> Unit,
    onPreviewSourceSelection: (PlaybackSelection) -> Unit,
    onPreviewDefaultSourceSelection: () -> Unit,
    onOpenSubtitleSettings: () -> Unit,
) {
    val initialDraft = remember(
        playback.sourceSelection,
        playback.preferredSourceQualityKey,
        playback.currentAdaptiveQuality,
        playback.sessionPlaybackSpeed,
        playback.currentSubtitle,
    ) {
        playback.toSettingsDraft()
    }
    var draft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var draftSubtitleTouched by remember(initialDraft) { mutableStateOf(false) }
    val resettingToDefaults = draft.resetToDefaults
    val draftDubMatchesActive = draft.sourceSelection.dubKey == playback.sourceSelection.dubKey
    val previewSelection = playback.preview.selection
    val usesPreviewOptions = resettingToDefaults || !draftDubMatchesActive
    val displayedSourceSelection = draft.sourceSelectionForDisplay(playback.preview)
    val sourceQualityOptions = if (usesPreviewOptions) {
        playback.preview.playbackData?.sourceQualities.orEmpty()
    } else {
        playback.playbackData.sourceQualities
    }
    val qualityOptionsLoading = usesPreviewOptions &&
        playback.isPreviewLoading &&
        previewSelection == draft.sourceSelection &&
        sourceQualityOptions.isEmpty()
    val previewSubtitles = if (usesPreviewOptions) {
        playback.preview.subtitles.orEmpty()
    } else {
        playback.subtitles
    }
    val subtitleOptions = if (!usesPreviewOptions) {
        (
            externalSubtitleOptions(playback.subtitles) + playback.subtitleOptions.filter {
                it.selection is VideoPlayerSubtitleSelection.Embedded
            }
            ).distinctBy { it.key }
    } else {
        externalSubtitleOptions(previewSubtitles)
    }
    val subtitleOptionsLoading = usesPreviewOptions &&
        playback.isPreviewLoading &&
        previewSelection == draft.sourceSelection &&
        playback.preview.subtitles == null
    val hasSelectableSubtitleOptions = subtitleOptions.any { it.selection != VideoPlayerSubtitleSelection.None }
    val selectedSubtitle = draft.subtitleSelection
    val hasPendingChanges = draft != initialDraft
    val streamOptionsEnabled = !usesPreviewOptions &&
        displayedSourceSelection.sourceQualityKey == playback.sourceSelection.sourceQualityKey

    LaunchedEffect(draft.sourceSelection.dubKey) {
        if (draftDubMatchesActive) {
            if (!resettingToDefaults) {
                draft = draft.copy(subtitleSelection = playback.currentSubtitle)
                draftSubtitleTouched = false
            }
            onPreviewSourceSelection(playback.sourceSelection)
        } else {
            if (!resettingToDefaults) {
                draft = draft.copy(
                    subtitleSelection = resolvePreviewSubtitleSelection(playback.currentSubtitle, previewSubtitles),
                )
                draftSubtitleTouched = false
            }
            onPreviewSourceSelection(draft.sourceSelection)
        }
    }

    LaunchedEffect(draftDubMatchesActive, previewSubtitles, playback.currentSubtitle) {
        if (draftDubMatchesActive) {
            if (!resettingToDefaults) {
                draft = draft.copy(subtitleSelection = playback.currentSubtitle)
                draftSubtitleTouched = false
            }
        } else if (!draftSubtitleTouched) {
            draft = draft.copy(
                subtitleSelection = resolvePreviewSubtitleSelection(playback.currentSubtitle, previewSubtitles),
            )
        }
    }

    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column {
            SheetHeader(
                title = stringResource(MR.strings.action_settings),
                menu = { close ->
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_reset),
                        onClick = {
                            close()
                            draft = defaultVideoPlayerSettingsDraft()
                            draftSubtitleTouched = true
                            onPreviewDefaultSourceSelection()
                        },
                    )
                },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
            ) {
                if (playback.playbackData.dubs.isNotEmpty()) {
                    item {
                        PlaybackOptionRow(
                            options = playback.playbackData.dubs,
                            titleRes = MR.strings.anime_playback_dub,
                            selectedKey = displayedSourceSelection.dubKey,
                            onSelect = {
                                draft = draft.copy(
                                    sourceSelection = draft.sourceSelection.withSelectedDub(
                                        it,
                                        initialDraft.sourceSelection,
                                    ),
                                    resetToDefaults = false,
                                )
                            },
                        )
                    }
                }

                if (playback.streamOptions.size > 1) {
                    item {
                        PlaybackOptionRow(
                            options = playback.streamOptions,
                            titleRes = MR.strings.anime_playback_stream,
                            selectedKey = displayedSourceSelection.streamKey,
                            enabled = streamOptionsEnabled,
                            onSelect = {
                                draft = draft.copy(
                                    sourceSelection = draft.sourceSelection.withSelectedStream(it),
                                    resetToDefaults = false,
                                )
                            },
                        )
                    }
                }

                if (qualityOptionsLoading || sourceQualityOptions.isNotEmpty()) {
                    item {
                        if (qualityOptionsLoading) {
                            LoadingPlaybackOptionRow(titleRes = MR.strings.anime_playback_source_quality)
                        } else {
                            PlaybackOptionRow(
                                options = sourceQualityOptions,
                                titleRes = MR.strings.anime_playback_source_quality,
                                selectedKey = displayedSourceSelection.sourceQualityKey,
                                onSelect = {
                                    draft = draft.copy(
                                        sourceSelection = draft.sourceSelection.withSelectedSourceQuality(
                                            it,
                                            initialDraft.sourceSelection,
                                        ),
                                        resetToDefaults = false,
                                    )
                                },
                            )
                        }
                    }
                }

                if (playback.showsAdaptiveQualitySelector) {
                    item {
                        ChipsRow(title = stringResource(MR.strings.anime_playback_quality)) {
                            playback.adaptiveQualities.forEach { option ->
                                FilterChip(
                                    label = option.label,
                                    selected = option.preference == draft.adaptiveQuality,
                                    onClick = {
                                        draft = draft.copy(
                                            adaptiveQuality = option.preference,
                                            resetToDefaults = false,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                item {
                    ChipsRow(title = stringResource(MR.strings.anime_playback_speed)) {
                        videoPlaybackSpeedOptions.forEach { option ->
                            FilterChip(
                                label = option.label,
                                selected = option.speed == draft.playbackSpeed,
                                onClick = {
                                    draft = draft.copy(
                                        playbackSpeed = option.speed,
                                        resetToDefaults = false,
                                    )
                                },
                            )
                        }
                    }
                }

                if (subtitleOptionsLoading || hasSelectableSubtitleOptions) {
                    item {
                        if (subtitleOptionsLoading) {
                            LoadingPlaybackOptionRow(titleRes = MR.strings.anime_playback_subtitles)
                        } else {
                            SubtitlePlaybackOptionRow(
                                options = subtitleOptions.map {
                                    VideoPlaybackOption(
                                        key = it.key,
                                        label = it.label,
                                    )
                                },
                                selectedKey = subtitleSelectionKey(selectedSubtitle),
                                onSelect = { key ->
                                    val selection = subtitleOptions
                                        .firstOrNull { option -> option.key == key }
                                        ?.selection
                                        ?: if (draftDubMatchesActive) {
                                            playback.currentSubtitle
                                        } else {
                                            defaultSubtitleSelection(previewSubtitles)
                                        }
                                    draft = draft.copy(
                                        subtitleSelection = selection,
                                        resetToDefaults = false,
                                    )
                                    draftSubtitleTouched = true
                                },
                                onOpenSubtitleSettings = if (hasSelectableSubtitleOptions) {
                                    onOpenSubtitleSettings
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s, Alignment.End),
            ) {
                Button(
                    label = stringResource(MR.strings.action_cancel),
                    onClick = onDismissRequest,
                    style = ButtonStyle.Text,
                )
                Button(
                    label = stringResource(MR.strings.action_apply),
                    enabled = hasPendingChanges,
                    onClick = {
                        onApplySettings(
                            draft.copy(
                                subtitleSelection = if (draftDubMatchesActive) {
                                    draft.subtitleSelection
                                } else {
                                    when (draft.subtitleSelection) {
                                        is VideoPlayerSubtitleSelection.Embedded -> {
                                            defaultSubtitleSelection(previewSubtitles)
                                        }
                                        else -> draft.subtitleSelection
                                    }
                                },
                            ),
                        )
                        onDismissRequest()
                    },
                )
            }
        }
    }
}

@Composable
private fun SubtitlePlaybackOptionRow(
    options: List<VideoPlaybackOption>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    onOpenSubtitleSettings: (() -> Unit)?,
) {
    Column {
        ChipsRow(title = stringResource(MR.strings.anime_playback_subtitles)) {
            options.forEach { option ->
                FilterChip(
                    label = option.label,
                    selected = option.key == selectedKey,
                    onClick = { onSelect(option.key) },
                )
            }
        }
        onOpenSubtitleSettings?.let { openSubtitleSettings ->
            SettingsRow(
                title = stringResource(MR.strings.anime_playback_subtitle_settings),
                onClick = openSubtitleSettings,
            )
        }
    }
}
