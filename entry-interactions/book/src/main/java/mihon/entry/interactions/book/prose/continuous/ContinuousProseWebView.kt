package mihon.entry.interactions.book.prose.continuous

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.util.system.isOutdated
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import tachiyomi.domain.entry.model.EntryChapter

internal data class ContinuousProseWebViewCallbacks(
    val onEvent: (ContinuousProseEvent, WebView) -> Unit,
    val onFailure: (String) -> Unit,
)

internal class ContinuousProseCommandSink {
    private var controller: ContinuousProseWebViewController? = null

    internal fun attach(controller: ContinuousProseWebViewController?) {
        this.controller = controller
    }

    fun seek(
        generation: Long,
        sectionKey: String,
        position: BookDocumentPosition,
        smooth: Boolean = true,
    ) {
        controller?.post(continuousProseSeekCommand(generation, sectionKey, position, smooth))
    }

    fun clearSelection(generation: Long) {
        controller?.post(continuousProseClearSelectionCommand(generation))
    }
}

@Composable
internal fun ContinuousProseWebView(
    projection: ContinuousProseProjection,
    sections: Map<String, BookDocumentSection<EntryChapter>>,
    settings: ContinuousProseRenderSettings,
    initialSectionKey: String,
    initialPosition: BookDocumentPosition,
    commandSink: ContinuousProseCommandSink,
    callbacks: ContinuousProseWebViewCallbacks,
    modifier: Modifier = Modifier,
) {
    val currentCallbacks = rememberUpdatedState(callbacks)
    // The controller is created by the AndroidView factory because constructing WebView requires its Context.
    val installedController = remember { arrayOfNulls<ContinuousProseWebViewController>(1) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ContinuousProseWebViewController(context).also { created ->
                installedController[0] = created
                commandSink.attach(created)
                created.callbacks = { currentCallbacks.value }
            }.webView
        },
        update = { view ->
            val active = installedController[0] ?: return@AndroidView
            active.callbacks = { currentCallbacks.value }
            active.update(
                projection = projection,
                sections = sections,
                command = continuousProseRenderCommand(
                    projection = projection,
                    settings = settings,
                    initialSectionKey = initialSectionKey,
                    initialPosition = initialPosition,
                ),
            )
            if (view.isOutdated()) {
                currentCallbacks.value.onFailure("The installed Android System WebView is outdated.")
            }
        },
        onRelease = {
            commandSink.attach(null)
            installedController[0]?.destroy()
            installedController[0] = null
        },
    )
    DisposableEffect(commandSink) {
        onDispose { commandSink.attach(null) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
internal class ContinuousProseWebViewController(context: Context) {
    private val validator = ContinuousProseMessageValidator()
    private val resourceClient = ContinuousProseResourceClient()
    private var messagePort: WebMessagePort? = null
    private var pendingCommand: String? = null
    private var renderedGeneration: Long? = null
    private var activeGeneration: Long = 0
    private var activeProjection: ContinuousProseProjection? = null
    var callbacks: () -> ContinuousProseWebViewCallbacks = {
        ContinuousProseWebViewCallbacks(onEvent = { _, _ -> }, onFailure = {})
    }

    val webView: WebView = WebView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            databaseEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }
        webViewClient = ReaderWebViewClient()
        loadDataWithBaseURL(
            "$CONTINUOUS_PROSE_ORIGIN/",
            CONTINUOUS_PROSE_WEB_SHELL,
            "text/html",
            "utf-8",
            null,
        )
    }

    fun update(
        projection: ContinuousProseProjection,
        sections: Map<String, BookDocumentSection<EntryChapter>>,
        command: String,
    ) {
        activeGeneration = projection.generation
        activeProjection = projection
        resourceClient.projection = projection
        resourceClient.sections = sections
        if (renderedGeneration == projection.generation) return
        renderedGeneration = projection.generation
        pendingCommand = command
        flushPendingCommand()
    }

    fun post(command: String) {
        messagePort?.postMessage(WebMessage(command))
    }

    fun destroy() {
        messagePort?.close()
        messagePort = null
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.destroy()
    }

    private fun connectBridge() {
        messagePort?.close()
        val ports = webView.createWebMessageChannel()
        messagePort = ports[0].apply {
            setWebMessageCallback(
                object : WebMessagePort.WebMessageCallback() {
                    override fun onMessage(port: WebMessagePort, message: WebMessage) {
                        val projection = activeProjection ?: return
                        val event = validator.parse(message.data.orEmpty(), projection) ?: return
                        if (event is ContinuousProseEvent.Ready) flushPendingCommand()
                        callbacks().onEvent(event, webView)
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
        webView.postWebMessage(
            WebMessage(activeGeneration.toString(), arrayOf(ports[1])),
            Uri.parse(CONTINUOUS_PROSE_ORIGIN),
        )
    }

    private fun flushPendingCommand() {
        val port = messagePort ?: return
        val command = pendingCommand ?: return
        pendingCommand = null
        port.postMessage(WebMessage(command))
    }

    private inner class ReaderWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            if (url == "$CONTINUOUS_PROSE_ORIGIN/") {
                connectBridge()
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? = resourceClient.intercept(request.url.toString())

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) callbacks().onFailure(error.description.toString())
        }

        override fun onSafeBrowsingHit(
            view: WebView,
            request: WebResourceRequest,
            threatType: Int,
            callback: SafeBrowsingResponse,
        ) {
            callback.backToSafety(true)
            callbacks().onFailure("The continuous reader was blocked by safe browsing.")
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            callbacks().onFailure("The continuous reader process stopped.")
            return true
        }
    }
}
