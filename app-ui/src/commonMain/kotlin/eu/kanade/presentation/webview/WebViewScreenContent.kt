package eu.kanade.presentation.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
 * [onUrlChange] follows the shown tab's address.
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
        val tabs = WebViewTabs()
        val events = object : BrowserEvents {
            override fun onUrlChange(page: BrowserPage, url: String) {
                tabs.onUrlChange(page, url)
            }

            override fun onPageFinished(page: BrowserPage, url: String?) {
                if (siteStorage != null && url != null) scope.launch { siteStorage.read(page, url) }
                scope.launch {
                    val html = page.evaluateJavascript(READ_HTML) ?: return@launch
                    tabs.onCloudflareCheck(page, "window._cf_chl_opt" in html || "Ray ID is" in html)
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
                if (isUserGesture) tabs.open(popup, url = null)
                return isUserGesture
            }

            override fun onJsDialog(dialog: BrowserJsDialog) {
                if (isActive) jsDialogs += dialog else dialog.answerUnseen()
            }
        }
        tabs.open(browserView.openPage(BrowserRequest(url, requestHeaders), userAgent, scope, events), url)
        tabs
    }
    val tab = tabs.shown
    val page = tab.page

    LaunchedEffect(tab.url) {
        tab.url?.let(currentOnUrlChange)
    }

    DisposableEffect(Unit) {
        onDispose {
            siteStorage?.last?.let { viewModel.siteStorageSource?.saveSiteStorage(it) }
            isActive = false
            jsDialogs.forEach { it.answerUnseen() }
            tabs.pages.forEach(BrowserPage::close)
        }
    }

    val closeTab = {
        if (tabs.count == 1) currentOnNavigateUp() else tabs.closeShown()
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = page.canGoBack || tabs.count > 1,
        onBackCompleted = { if (page.canGoBack) page.goBack() else closeTab() },
    )

    Scaffold(
        topBar = {
            WebViewTopBar(
                page = page,
                initialTitle = initialTitle,
                url = tab.url,
                showCloudflareHelp = tab.showsCloudflareHelp,
                onNavigateUp = onNavigateUp,
                onCloseTab = closeTab.takeIf { tabs.count > 1 },
                onShare = tab.onUrl { viewModel.shareWebpage(context, it) }.takeIf { viewModel.canShare },
                onOpenInBrowser = tab.onUrl { viewModel.openInBrowser(context, it) },
                onClearCookies = tab.onUrl(viewModel::clearCookies),
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

// A popup that hasn't reported an address yet has none to act on
private fun WebViewTab.onUrl(action: (String) -> Unit): () -> Unit = { url?.let(action) }

// Answers a dialog the user can no longer see as a browser does when its page is closing
private fun BrowserJsDialog.answerUnseen() {
    when (this) {
        is BrowserJsDialog.Alert -> confirm()
        is BrowserJsDialog.Confirm, is BrowserJsDialog.Prompt -> cancel()
    }
}

private const val READ_HTML = "document.documentElement.outerHTML"
