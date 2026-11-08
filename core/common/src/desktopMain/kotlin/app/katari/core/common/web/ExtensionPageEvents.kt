package app.katari.core.common.web

import java.io.InputStream

/**
 * What an [ExtensionPage] tells about itself, on threads of the page's own; nothing comes once it is closed. Only the
 * main frame's loads start and finish; errors come from every frame.
 */
interface ExtensionPageEvents {
    fun onPageStarted(url: String)

    /** The main frame finished loading [url]; the cookies the page has set are in the app's cookie jar by now. */
    fun onPageFinished(url: String)

    fun onTitleChanged(title: String)

    /**
     * A frame's load of [url] failed before any response, with Chromium's network [errorCode] (such as `-105` for a
     * host that can't be resolved) and its [description].
     */
    fun onLoadFailed(url: String, isForMainFrame: Boolean, errorCode: Int, description: String)

    /**
     * What answers [request] instead of the network; null lets it go to the network. Called on the thread Chromium
     * asks from, which waits for the answer before any of the page's requests goes on, so it must not wait long.
     */
    fun interceptRequest(request: ExtensionPageRequest): ExtensionPageResponse?

    fun onConsoleMessage(level: ConsoleLevel, message: String, sourceId: String, lineNumber: Int)

    /**
     * A script called [method] of the [bridge][ExtensionPage.addBridge] [name] with [arguments], a JSON array of the
     * values it passed (`undefined` as `null`).
     */
    fun onBridgeCall(name: String, method: String, arguments: String)

    /** The page's renderer ended, [crashed] or killed, or Chromium did; nothing more comes from the page. */
    fun onPageGone(crashed: Boolean)

    enum class ConsoleLevel { DEBUG, LOG, WARNING, ERROR }
}

class ExtensionPageRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val isForMainFrame: Boolean,
)

/**
 * An answer to an [ExtensionPageRequest]. A [body] of null answers with no content, as for a request that was
 * refused.
 */
class ExtensionPageResponse(
    val statusCode: Int,
    val reasonPhrase: String,
    val mimeType: String?,
    val encoding: String?,
    val headers: Map<String, String>,
    val body: InputStream?,
)
