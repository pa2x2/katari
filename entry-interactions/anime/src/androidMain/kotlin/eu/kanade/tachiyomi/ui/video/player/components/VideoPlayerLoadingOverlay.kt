package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel.LoadingStep
import kotlinx.coroutines.delay
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.MediaBackdrop
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The wait before an episode's first frame; the heading shows once the title is known, so there is a way back. A wait
 * that runs past [STATUS_DELAY_MS] says what it waits for under the spinner, and offers [sourceChoice] when the source
 * has listed another server, dub, quality or stream to try instead.
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
    var waitedLong by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(STATUS_DELAY_MS)
        waitedLong = true
    }
    val spacing = KatariTheme.spacing
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        MediaBackdrop(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .widthIn(max = KatariTheme.sizes.readableWidth)
                .padding(horizontal = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            LoadingIndicator(tone = ContentTone.OnMedia)
            if (waitedLong) {
                step?.let {
                    Text(
                        text = it.label(),
                        role = TextRole.Supporting,
                        tone = ContentTone.OnMediaVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                sourceChoice?.let { SourceChoiceButton(it, style = ButtonStyle.Text) }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .videoPlayerSafeContentPadding(includeTop = true)
                .padding(spacing.s),
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
    is LoadingStep.Opening -> when {
        server == null -> stringResource(MR.strings.anime_playback_opening_stream)
        failedServer == null -> stringResource(MR.strings.anime_playback_opening_server, server)
        else -> stringResource(MR.strings.anime_playback_trying_server, failedServer, server)
    }
}

// Most episodes open sooner, so the spinner alone covers them.
private const val STATUS_DELAY_MS = 3_000L
