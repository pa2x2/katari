package eu.kanade.presentation.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import cafe.adriel.voyager.core.stack.mutableStateStackOf
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.tachiyomi.ui.webview.WebViewViewModel
import eu.kanade.tachiyomi.ui.webview.browser.BrowserEvents
import eu.kanade.tachiyomi.ui.webview.browser.BrowserJsDialog
import eu.kanade.tachiyomi.ui.webview.browser.BrowserPage
import eu.kanade.tachiyomi.ui.webview.browser.BrowserRequest
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.core.platform.currentPlatformContext
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*

/**
 * A site in [browserView]: [url], loaded with [headers] and [sourceId]'s web view headers, and the popups it opens over
 * it as tabs. [sourceId]'s source gets the last site storage read on the way, if it asks for it, as the screen closes.
 */
@Composable
fun WebViewScreenContent(
    browserView: BrowserView,
    url: String,
    initialTitle: String?,
    sourceId: Long?,
    headers: Map<String, String>,
    onNavigateUp: () -> Unit,
    onUrlChange: (String) -> Unit,
) {
    val viewModel = assistedMetroViewModel<WebViewViewModel, WebViewViewModel.Factory> { create(sourceId) }
    val context = currentPlatformContext()
    val scope = rememberCoroutineScope()
    val messages = rememberScreenMessages()
    val currentOnNavigateUp by rememberUpdatedState(onNavigateUp)
    val currentOnUrlChange by rememberUpdatedState(onUrlChange)

    var currentUrl by remember { mutableStateOf(url) }
    var showCloudflareHelp by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }
    var renderProcessGone by remember { mutableStateOf(false) }
    val jsDialogs = remember { mutableStateListOf<BrowserJsDialog>() }
    // Read after each page load and handed over once, as the screen closes
    val siteStorage = remember { viewModel.siteStorageSource?.let { SiteStorageCapture() } }

    val tabs = remember {
        val requestHeaders = viewModel.headers + headers
        val userAgent = requestHeaders.entries
            .lastOrNull { it.key.equals("User-Agent", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotEmpty() }
            ?: viewModel.defaultUserAgentProvider()
        val tabs = mutableStateStackOf<BrowserPage>()
        val events = object : BrowserEvents {
            override fun onUrlChange(url: String) {
                currentUrl = url
                currentOnUrlChange(url)
            }

            override fun onPageFinished(page: BrowserPage, url: String?) {
                if (siteStorage != null && url != null) scope.launch { siteStorage.read(page, url) }
                scope.launch {
                    val html = page.evaluateJavascript(READ_HTML) ?: return@launch
                    showCloudflareHelp = "window._cf_chl_opt" in html || "Ray ID is" in html
                }
            }

            override fun onRenderProcessGone(crashed: Boolean) {
                logcat(if (crashed) LogPriority.ERROR else LogPriority.WARN) {
                    "WebView renderer process exited; closing the affected WebView screen"
                }
                if (!renderProcessGone) {
                    renderProcessGone = true
                    messages.show(MR.strings.information_webview_crashed, long = true)
                    currentOnNavigateUp()
                }
            }

            override fun onNewWindow(popup: BrowserPage, isUserGesture: Boolean): Boolean {
                // A popup the user didn't ask for is ignored, as a browser would
                if (isUserGesture) tabs.push(popup)
                return isUserGesture
            }

            override fun onJsDialog(dialog: BrowserJsDialog) {
                if (isActive) jsDialogs += dialog else dialog.answerUnseen()
            }
        }
        tabs.push(browserView.openPage(BrowserRequest(url, requestHeaders), userAgent, scope, events))
        tabs
    }
    val page = tabs.lastItemOrNull!!

    DisposableEffect(Unit) {
        onDispose {
            siteStorage?.last?.let { viewModel.siteStorageSource?.saveSiteStorage(it) }
            isActive = false
            jsDialogs.forEach { it.answerUnseen() }
            tabs.items.forEach(BrowserPage::close)
        }
    }

    val closeTab = {
        if (tabs.size == 1) {
            currentOnNavigateUp()
        } else {
            val closed = page
            tabs.pop()
            closed.close()
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = page.canGoBack || tabs.size > 1,
        onBackCompleted = { if (page.canGoBack) page.goBack() else closeTab() },
    )

    Scaffold(
        topBar = {
            WebViewTopBar(
                page = page,
                initialTitle = initialTitle,
                url = currentUrl,
                showCloudflareHelp = showCloudflareHelp,
                onNavigateUp = onNavigateUp,
                onCloseTab = closeTab.takeIf { tabs.size > 1 },
                onShare = { viewModel.shareWebpage(context, currentUrl) }.takeIf { viewModel.canShare },
                onOpenInBrowser = { viewModel.openInBrowser(context, currentUrl) },
                onClearCookies = { viewModel.clearCookies(currentUrl) },
            )
        },
    ) { contentPadding ->
        // Keyed to the page, so switching tabs shows the other page's browser rather than loading it into this one
        key(page) {
            browserView.Page(
                page = page,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }

    jsDialogs.firstOrNull()?.let { dialog ->
        key(dialog) {
            WebViewJsDialog(dialog, onAnswered = { jsDialogs.remove(dialog) })
        }
    }
}

// Answers a dialog the user can no longer see as a browser does when its page is closing
private fun BrowserJsDialog.answerUnseen() {
    when (this) {
        is BrowserJsDialog.Alert -> confirm()
        is BrowserJsDialog.Confirm, is BrowserJsDialog.Prompt -> cancel()
    }
}

private const val READ_HTML = "document.documentElement.outerHTML"
