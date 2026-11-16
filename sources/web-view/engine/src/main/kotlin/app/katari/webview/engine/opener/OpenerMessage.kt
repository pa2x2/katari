package app.katari.webview.engine.opener

/** What a page's [OpenerScript] asks the app to carry to the windows it is linked with. */
sealed interface OpenerMessage {
    /**
     * The page opened its popup [window] at [url], or, where the page already has a popup of that id, loaded [url] in
     * it, as `window.open` does with the name of an open window.
     */
    data class Open(val window: String, val url: String) : OpenerMessage

    /**
     * The page, of [origin], posted [data], as JSON, to its popup [window], or to its opener where [window] is null.
     * The receiving page drops it unless its origin is [targetOrigin] or that is `*`. [ports] are the ids of the
     * message ports the message took along, whose messages then go between the two pages ([PortPost]).
     */
    data class Post(
        val window: String?,
        val data: String,
        val targetOrigin: String,
        val origin: String,
        val ports: List<String>,
    ) : OpenerMessage

    /** The page posted [data], as JSON, through [port], one a [Post] took between it and another page. */
    data class PortPost(val port: String, val data: String) : OpenerMessage

    /** The page closed its popup [window], or itself, which only a popup does, where [window] is null. */
    data class Close(val window: String?) : OpenerMessage
}
