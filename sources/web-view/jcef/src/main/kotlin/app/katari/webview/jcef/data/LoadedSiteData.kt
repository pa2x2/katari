package app.katari.webview.jcef.data

import app.katari.webview.engine.devtools.DevToolsSession
import app.katari.webview.jcef.devtools.JcefDevToolsSession
import app.katari.webview.jcef.surface.PageFrame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.cef.CefApp
import org.cef.CefBrowserSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefRendering
import org.cef.handler.CefLifeSpanHandlerAdapter
import kotlin.time.Duration.Companion.seconds

/**
 * Clears what a running Chromium holds of sites in memory, through the DevTools protocol of a blank browser opened for
 * it: the HTTP cache, and the storage of [origins] but their cookies, which stay the app's cookie store's to clear.
 */
internal object LoadedSiteData {
    suspend fun clear(app: CefApp, origins: Set<String>) {
        val client = app.createClient()
        val created = CompletableDeferred<CefBrowser>()
        val closed = CompletableDeferred<Unit>()
        client.addLifeSpanHandler(
            object : CefLifeSpanHandlerAdapter() {
                override fun onAfterCreated(browser: CefBrowser) {
                    created.complete(browser)
                }

                override fun onBeforeClose(browser: CefBrowser) {
                    closed.complete(Unit)
                }
            },
        )
        val browser = client.createBrowser(
            "",
            CefRendering.CefRenderingWithHandler(PageFrame(), null),
            false,
            null,
            CefBrowserSettings(),
        )
        browser.setCloseAllowed()
        browser.createImmediately()
        try {
            withTimeout(BROWSER_TIMEOUT) { created.await() }
            val session = JcefDevToolsSession(browser)
            try {
                session.call("Network.clearBrowserCache")
                origins.forEach { origin ->
                    session.call(
                        "Storage.clearDataForOrigin",
                        buildJsonObject {
                            put("origin", origin)
                            put("storageTypes", SITE_STORAGE)
                        },
                    )
                }
            } finally {
                session.close()
            }
        } finally {
            browser.close(true)
            withTimeoutOrNull(BROWSER_TIMEOUT) { closed.await() }
            client.dispose()
        }
    }

    private val BROWSER_TIMEOUT = 5.seconds

    // Every kind of storage the protocol names but cookies
    private const val SITE_STORAGE = "file_systems,indexeddb,local_storage,shader_cache,websql,service_workers," +
        "cache_storage,interest_groups,shared_storage,storage_buckets"
}
