package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.ui.di.LocalAppUiGraph
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.i18n.*

/** What the Clear WebView data row does: clears the web view's caches and storage and tells how it went. */
@Composable
fun rememberClearWebViewData(): () -> Unit {
    val context = currentPlatformContext()
    val webViewData = LocalAppUiGraph.current.webViewData
    val messages = rememberScreenMessages()
    val scope = rememberCoroutineScope()
    return {
        try {
            webViewData.clearCaches(context)
            scope.launchNonCancellable {
                webViewData.deleteStorage(context)
                withUIContext { messages.show(MR.strings.webview_data_deleted) }
            }
        } catch (e: Throwable) {
            logcat(LOG_TAG, LogPriority.ERROR) { e.asLog() }
            messages.show(MR.strings.cache_delete_error)
        }
    }
}

private const val LOG_TAG = "WebViewData"
