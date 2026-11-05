package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoSubtitleEditorOverlay
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.tracks.subtitleKeepsOwnStyle
import mihon.video.engine.spi.player.SubtitleCue
import mihon.video.engine.spi.player.VideoPlayer
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The subtitle editor over the paused video. It previews the text cues on screen, or a sample line when there are none,
 * and resumes playback on close if the video was playing when it opened. Whether subtitles stay in the picture is the
 * viewer's, kept for every title, while their appearance is the title's.
 */
@Composable
internal fun VideoPlayerSubtitleEditorLayer(
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    currentPlayer: VideoPlayer,
    isInPictureInPictureMode: Boolean,
) {
    if (ui.subtitleEditorVisible && !isInPictureInPictureMode) {
        val playerState by currentPlayer.state.collectAsState()
        val sampleCue = stringResource(MR.strings.anime_playback_subtitle_sample)
        VideoSubtitleEditorOverlay(
            draftAppearance = ui.subtitleEditorDraft,
            keepInPicture = ui.subtitleEditorKeepsInPicture,
            aspectRatio = host.visit.videoAspectRatio,
            framing = current.playback.pictureFraming,
            subtitleKeepsOwnStyle = playerState.subtitleKeepsOwnStyle(),
            previewCues = playerState.cues.filter { it.image == null && it.text.isNotBlank() }.ifEmpty {
                listOf(SubtitleCue(sampleCue))
            },
            onDraftChange = { ui.subtitleEditorDraft = it.normalized() },
            onKeepInPictureChange = { ui.subtitleEditorKeepsInPicture = it },
            onDismissRequest = {
                actions.endSpeedBoost()
                ui.subtitleEditorVisible = false
                if (ui.resumePlaybackAfterSubtitleEditor) {
                    currentPlayer.play()
                }
            },
            onReset = {
                ui.subtitleEditorDraft = VideoSubtitleAppearance()
                ui.subtitleEditorKeepsInPicture = false
            },
            onDone = {
                actions.endSpeedBoost()
                val normalizedAppearance = ui.subtitleEditorDraft.normalized()
                ui.subtitleEditorVisible = false
                viewModel.updateSubtitleAppearance(normalizedAppearance)
                host.visit.subtitlesInPicture = ui.subtitleEditorKeepsInPicture
                host.preferences.subtitlesInPicture.set(ui.subtitleEditorKeepsInPicture)
                if (ui.resumePlaybackAfterSubtitleEditor) {
                    currentPlayer.play()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
