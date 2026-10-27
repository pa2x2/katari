package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.components.VideoSubtitleEditorOverlay
import eu.kanade.tachiyomi.ui.video.player.subtitlePreviewCues
import eu.kanade.tachiyomi.ui.video.player.subtitlePreviewText
import eu.kanade.tachiyomi.ui.video.player.tracks.subtitleKeepsOwnStyle
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * The subtitle editor over the paused video. It previews the cues on screen, or a sample line when there are none,
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
    currentPlayer: Player,
    isInPictureInPictureMode: Boolean,
) {
    val context = LocalContext.current
    if (ui.subtitleEditorVisible && !isInPictureInPictureMode) {
        VideoSubtitleEditorOverlay(
            draftAppearance = ui.subtitleEditorDraft,
            keepInPicture = ui.subtitleEditorKeepsInPicture,
            aspectRatio = host.visit.videoAspectRatio,
            framing = current.playback.pictureFraming,
            subtitleKeepsOwnStyle = currentPlayer.subtitleKeepsOwnStyle(),
            previewCues = currentPlayer.subtitlePreviewCues().ifEmpty {
                listOf(
                    Cue.Builder()
                        .setText(context.stringResource(MR.strings.anime_playback_subtitle_sample))
                        .build(),
                )
            },
            previewText = currentPlayer.subtitlePreviewText()
                ?: context.stringResource(MR.strings.anime_playback_subtitle_sample),
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
