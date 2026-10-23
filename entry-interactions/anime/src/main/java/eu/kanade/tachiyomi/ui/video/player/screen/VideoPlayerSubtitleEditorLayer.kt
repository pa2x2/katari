package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.text.Cue
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.components.VideoSubtitleEditorOverlay
import eu.kanade.tachiyomi.ui.video.player.subtitlePreviewCues
import eu.kanade.tachiyomi.ui.video.player.subtitlePreviewText
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/**
 * The subtitle editor over the paused video. It previews the cues on screen, or a sample line when there are none,
 * and resumes playback on close if the video was playing when it opened.
 */
@Composable
internal fun VideoPlayerSubtitleEditorLayer(
    ui: VideoPlayerWatchUiState,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    currentPlayer: ExoPlayer,
    isInPictureInPictureMode: Boolean,
) {
    val context = LocalContext.current
    if (ui.subtitleEditorVisible && !isInPictureInPictureMode) {
        VideoSubtitleEditorOverlay(
            draftAppearance = ui.subtitleEditorDraft,
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
            onDismissRequest = {
                actions.endSpeedBoost()
                ui.subtitleEditorVisible = false
                if (ui.resumePlaybackAfterSubtitleEditor) {
                    currentPlayer.play()
                }
            },
            onReset = { ui.subtitleEditorDraft = VideoSubtitleAppearance() },
            onDone = {
                actions.endSpeedBoost()
                val normalizedAppearance = ui.subtitleEditorDraft.normalized()
                ui.subtitleEditorVisible = false
                viewModel.updateSubtitleAppearance(normalizedAppearance)
                if (ui.resumePlaybackAfterSubtitleEditor) {
                    currentPlayer.play()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
