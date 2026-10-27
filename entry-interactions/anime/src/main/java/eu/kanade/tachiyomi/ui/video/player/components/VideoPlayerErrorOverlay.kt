package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoProblemAction
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Public
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An episode that couldn't open or stopped with an error, with the actions that may fix it. [failedServers] are the
 * names of the servers it failed on, which the text lists when another server is the way out; [action] opens where
 * the problem is fixed outside the player.
 */
@Composable
internal fun VideoPlayerErrorOverlay(
    problem: VideoPlayerProblem,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenWebView: (() -> Unit)? = null,
    sourceChoice: SourceChoiceAction? = null,
    heading: VideoPlayerHeadingText? = null,
    failedServers: List<String> = emptyList(),
    action: VideoProblemAction? = null,
    onAction: (VideoProblemAction) -> Unit = {},
) {
    VideoPlayerProblemFrame(
        title = stringResource(problem.cause.title),
        explanation = problemExplanation(problem.cause, sourceChoice?.choice, failedServers, action),
        onBack = onBack,
        modifier = modifier,
        heading = heading,
        details = problem.details,
    ) {
        sourceChoice?.let { SourceChoiceButton(it) }
        action?.let { VideoProblemActionButton(it) { onAction(it) } }
        onOpenWebView?.takeIf { problem.cause.offersWebView }?.let {
            Button(
                label = stringResource(MR.strings.action_open_in_web_view),
                onClick = it,
                style = ButtonStyle.Tonal,
                icon = MaterialSymbols.Rounded.Public,
            )
        }
        Button(label = stringResource(MR.strings.action_retry), onClick = onRetry)
    }
}
