package app.katari.core.common.web

/**
 * The pages extensions' own web views run on desktop, where the Android stand-ins' `android.webkit.WebView` drives
 * one each. A page is never drawn, as a challenge page isn't. It sends the app's cookies and leaves the ones it sets in
 * the app's cookie jar ([ChallengePage] says how), and it sends [its user agent][ExtensionPageSettings.userAgent] from
 * every frame, other sites' frames among them.
 */
interface ExtensionPages {
    /** Whether this run has such pages; where it hasn't, an extension's web view fails as it would without one. */
    val isOffered: Boolean

    /** The user agent of a page whose extension sets none: the one the app's own requests send. */
    val defaultUserAgent: String

    /**
     * A page that tells [events] what happens on it until it is closed. Nothing starts until the first call on it, so
     * a page that is closed before it loads anything costs nothing. Called only where [isOffered].
     */
    fun open(settings: ExtensionPageSettings, events: ExtensionPageEvents): ExtensionPage
}

/**
 * One page, to be called from any thread: calls take effect in the order they were made, after the page has opened,
 * and none but [close] waits for it. Once closed, calls do nothing.
 */
interface ExtensionPage {
    fun apply(settings: ExtensionPageSettings)

    /** Loads [url], sending [headers] with this load's request only. */
    fun load(url: String, headers: Map<String, String>)

    /**
     * Loads [content] as the document at [baseUrl], which its scripts take for their origin and its relative links
     * resolve against; nothing is fetched from [baseUrl] for it. A null [baseUrl] gives the document no origin.
     */
    fun loadContent(baseUrl: String?, content: String, mimeType: String, encoding: String)

    /**
     * Runs [script] in the main frame as its own scripts run and hands [result] its value as JSON, `null` where it has
     * none or threw, on a thread of the page's own. A page closed before then hands nothing.
     */
    fun evaluate(script: String, result: ((String) -> Unit)?)

    /**
     * Gives the documents that load from now on an object [name] whose [methods] post their arguments to
     * [ExtensionPageEvents.onBridgeCall]. The calls return nothing to the page, and they arrive in the order made.
     * Adding a name again replaces its methods.
     */
    fun addBridge(name: String, methods: Set<String>)

    fun removeBridge(name: String)

    /** Hands the page [touch] as a touchscreen's input, which its documents can't tell from a person's own. */
    fun touch(touch: ExtensionPageTouch)

    fun stopLoading()

    fun reload()

    /**
     * Closes the page and ends its renderer. Unlike the other calls, it waits a bounded time: it returns once the
     * cookies the page set are in the app's cookie jar, as an Android web view's are once it is destroyed.
     */
    fun close()
}

/**
 * How a page behaves, as an Android web view's settings say.
 *
 * @property userAgent sent with every request the page and its frames make, and shown to their scripts.
 * @property blocksImages whether images are kept from loading over the network, as Android's `blockNetworkImage`
 * keeps them; an image [ExtensionPageEvents.interceptRequest] answers still loads.
 */
data class ExtensionPageSettings(
    val userAgent: String,
    val javaScriptEnabled: Boolean,
    val blocksImages: Boolean,
)
