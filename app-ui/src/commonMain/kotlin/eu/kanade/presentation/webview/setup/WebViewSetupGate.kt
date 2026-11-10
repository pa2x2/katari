package eu.kanade.presentation.webview.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.EmptyScreenAction
import app.katari.presentation.kit.feedback.LoadingScreen
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.shortFileSize
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Shows [content], a web page, once the web view is set up, and until then what stands between the user and it: the
 * question whether to set it up, the download's progress with a way to stop it, or why it failed with a way to try
 * again. The set-up goes on when this leaves the screen. [onDeclined] follows the user putting the set-up off or
 * stopping it, which leaves the page nothing to show.
 */
@Composable
fun WebViewSetupGate(
    setup: WebViewSetup,
    onDeclined: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state by setup.state.collectAsStateWithLifecycle()
    when (val current = state) {
        WebViewSetupState.Ready -> content()
        WebViewSetupState.NotSetUp -> WebViewSetupDialog(setup, onDownload = setup::start, onDismiss = onDeclined)
        is WebViewSetupState.Downloading -> LoadingScreen(
            modifier = modifier,
            message = stringResource(
                MR.strings.webview_setup_downloading,
                shortFileSize(current.downloadedBytes),
                shortFileSize(current.totalBytes),
            ),
            progress = { current.downloadedBytes.toFloat() / current.totalBytes },
            action = {
                Button(
                    label = stringResource(MR.strings.action_cancel),
                    onClick = {
                        setup.cancel()
                        onDeclined()
                    },
                    style = ButtonStyle.Outlined,
                )
            },
        )
        WebViewSetupState.Unpacking -> LoadingScreen(
            modifier = modifier,
            message = stringResource(MR.strings.webview_setup_unpacking),
        )
        is WebViewSetupState.Failed -> EmptyScreen(
            message = stringResource(MR.strings.webview_setup_failed, webViewSetupFailureText(current.failure)),
            modifier = modifier,
            actions = listOf(EmptyScreenAction(MR.strings.action_retry, MaterialSymbols.Rounded.Refresh, setup::start)),
        )
    }
}
