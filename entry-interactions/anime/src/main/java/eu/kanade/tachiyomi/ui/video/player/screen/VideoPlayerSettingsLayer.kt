package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.media3.common.Player
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEngineChoice
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettings
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsContent
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.engine.MpvAvailability

/**
 * The player settings: speed, quality, subtitles, audio, source and the title's player engine. Applying a source or
 * resetting saves the progress first; the subtitle settings close the settings, pause the video and open the subtitle
 * editor.
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
) {
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
            modifier = settingsModifier,
        )
    }
}
