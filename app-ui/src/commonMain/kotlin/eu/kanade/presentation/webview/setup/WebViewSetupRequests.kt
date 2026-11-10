package eu.kanade.presentation.webview.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupState

/**
 * The set-up dialog over whatever the window shows, for requests that come from no screen that shows it itself: the
 * Library's notice, a notification, a source's error. Composed once, by the window. A download the user agrees to
 * here runs behind whatever they go on to do.
 */
@Composable
fun WebViewSetupRequests(setup: WebViewSetup) {
    val isRequested by setup.isRequested.collectAsStateWithLifecycle()
    val state by setup.state.collectAsStateWithLifecycle()
    if (!isRequested || state != WebViewSetupState.NotSetUp) return
    WebViewSetupDialog(
        setup = setup,
        onDownload = {
            setup.dismissRequest()
            setup.start()
        },
        onDismiss = setup::dismissRequest,
    )
}
