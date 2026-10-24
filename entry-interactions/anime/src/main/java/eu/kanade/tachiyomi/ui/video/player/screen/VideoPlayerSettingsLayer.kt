package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.media3.common.Player
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEngineChoice
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerMarkerSkipChoice
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettings
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsContent
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.engine.MpvAvailability
import eu.kanade.tachiyomi.ui.video.player.markers.skippable
import kotlinx.coroutines.launch
import mihon.entry.interactions.settings.AnimeMarkerSkipMode
import mihon.entry.viewer.settings.ViewerSettingBinding

/**
 * The player settings: speed, quality, subtitles, audio, source, and the title's skip mode and player engine. Applying
 * a source or resetting saves the progress first; the subtitle settings close the settings, pause the video and open
 * the subtitle editor.
 */
@Composable
internal fun VideoPlayerSettingsLayer(
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    currentPlayer: Player,
    settingsSection: VideoPlayerSettingsSection?,
    isInPictureInPictureMode: Boolean,
    engine: WatchedPlayerEngine,
    markerSkipSetting: ViewerSettingBinding<AnimeMarkerSkipMode>?,
) {
    val scope = rememberCoroutineScope()
    val markerSkipOverride = markerSkipSetting?.let { it.state.collectAsState().value.entryOverride }
    val streamHasSkips = current.stream.markers.any { it.skippable }
    VideoPlayerSettings(
        visible = ui.settingsVisible && !isInPictureInPictureMode && !ui.subtitleEditorVisible,
        onDismissRequest = actions.dismissSettings,
    ) { settingsFrame, settingsModifier ->
        VideoPlayerSettingsContent(
            frame = settingsFrame,
            playback = current.playback,
            initialSection = settingsSection,
            onDismissRequest = actions.dismissSettings,
            onSelectSpeed = { speed ->
                actions.endSpeedBoost()
                viewModel.updateSessionPlaybackSpeed(speed)
            },
            onSelectAdaptiveQuality = viewModel::selectAdaptiveQuality,
            onSelectSubtitle = viewModel::selectSubtitle,
            onSelectAudioTrack = viewModel::selectAudioTrack,
            onApplySource = { selection, subtitle ->
                host.playback.persist(currentPlayer)
                // Selected first: applying the source carries the current subtitle over.
                subtitle?.let(viewModel::selectSubtitle)
                viewModel.applySourceSelection(selection)
            },
            onReset = {
                actions.endSpeedBoost()
                host.playback.persist(currentPlayer)
                viewModel.resetPlayerSettings()
            },
            onPreviewSourceSelection = viewModel::previewSourceSelection,
            onOpenSubtitleSettings = {
                actions.endSpeedBoost()
                ui.settingsVisible = false
                ui.episodesDrawerVisible = false
                ui.subtitleEditorDraft = current.playback.subtitleAppearance
                ui.resumePlaybackAfterSubtitleEditor = currentPlayer.isPlaying
                currentPlayer.pause()
                ui.subtitleEditorVisible = true
            },
            engine = VideoPlayerEngineChoice(
                override = current.playback.engineOverride,
                autoReason = engine.run.reason,
                mpvUsable = engine.mpv == MpvAvailability.Available,
            ),
            onSelectEngine = { choice -> engine.choose(choice, currentPlayer.currentPosition) },
            markerSkip = VideoPlayerMarkerSkipChoice(markerSkipOverride).takeIf {
                markerSkipSetting != null && streamHasSkips
            },
            onSelectMarkerSkip = { mode ->
                val setting = markerSkipSetting ?: return@VideoPlayerSettingsContent
                scope.launch {
                    if (mode == null) setting.clearEntryOverride() else setting.setEntryOverride(mode)
                }
            },
            modifier = settingsModifier,
        )
    }
}
