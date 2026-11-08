package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.surface.MediaBackdrop
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel.LoadingStep
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The wait before an episode's first frame; the heading shows once the title is known, so there is a way back. Under
 * the spinner it says what it waits for, and offers [sourceChoice] when the source has listed another server, dub,
 * quality or stream to try instead.
 */
@Composable
internal fun VideoPlayerLoadingOverlay(
    videoTitle: String?,
    episodeName: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    step: LoadingStep? = null,
    sourceChoice: SourceChoiceAction? = null,
) {
    Box(modifier = modifier) {
        MediaBackdrop(modifier = Modifier.matchParentSize())
        LoadingScreen(
            modifier = Modifier.matchParentSize(),
            message = step?.label(),
            onMedia = true,
            action = sourceChoice?.let { choice -> { SourceChoiceButton(choice, style = ButtonStyle.Text) } },
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .videoPlayerSafeContentPadding(includeTop = true)
                .padding(KatariTheme.spacing.s),
        ) {
            VideoPlayerHeading(
                videoTitle = videoTitle.orEmpty(),
                episodeName = episodeName.orEmpty(),
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun LoadingStep.label(): String = when (this) {
    is LoadingStep.AskingSource -> stringResource(MR.strings.anime_playback_asking_source, sourceName)
    is LoadingStep.Opening ->
        server
            ?.let { stringResource(MR.strings.anime_playback_opening_server, it) }
            ?: stringResource(MR.strings.anime_playback_opening_stream)
    is LoadingStep.FallingBack -> stringResource(
        MR.strings.anime_playback_trying_server,
        notice.failedServer,
        notice.playingServer,
        notice.attempt,
        notice.attempts,
    )
}
