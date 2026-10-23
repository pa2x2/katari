package mihon.video.engine.mpv.proxy

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Base64

/**
 * The proxy's URLs, all under `http://127.0.0.1:<port>/<token>/`:
 * - `r/<headers>/<scheme>/<host[:port]>/<path>?<query>` is a remote URL, fetched with the header set `<headers>`. The
 *   upstream URL stays a path, so a relative reference in a playlist or manifest that mpv resolves against it stays
 *   on the proxy and on the same upstream directory;
 * - `f/<base64url of a content:// or file:// URI>/<file name>` is a local file. The name only helps format probing.
 */
internal class ProxyRoutes(port: Int, private val token: String) {

    private val root = "http://127.0.0.1:$port/$token/"

    fun remote(headers: HeaderSet, url: HttpUrl): String = buildString {
        append(root).append(REMOTE).append('/').append(headers.id).append('/')
        append(url.scheme).append('/').append(url.host)
        if (url.port != HttpUrl.defaultPort(url.scheme)) append(':').append(url.port)
        append(url.encodedPath)
        url.encodedQuery?.let { append('?').append(it) }
    }

    /**
     * The proxy URL of [reference] as written in a playlist or manifest at [base], which is where it was fetched from
     * after redirects; null for a reference the proxy can't fetch, such as a `data:` key.
     */
    fun remoteReference(headers: HeaderSet, base: HttpUrl, reference: String): String? =
        base.resolve(reference)
            ?.takeIf { it.scheme == "http" || it.scheme == "https" }
            ?.let { remote(headers, it) }

    fun local(uri: String, name: String): String =
        root + LOCAL + "/" + Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray()) + "/" +
            name.ifEmpty { "file" }.replace(UNSAFE_NAME, "_")

    /** What a request path names, or null when it isn't one of this session's URLs. */
    fun parse(path: String, query: String?): ProxyTarget? {
        val parts = path.removePrefix("/").split('/', limit = 3)
        if (parts.size < 3 || !constantTimeEquals(parts[0], token)) return null
        return when (parts[1]) {
            REMOTE -> {
                val rest = parts[2].split('/', limit = 4)
                if (rest.size < 3) return null
                val headers = HeaderSet.of(rest[0]) ?: return null
                val scheme = rest[1].takeIf { it == "http" || it == "https" } ?: return null
                val upstream = "$scheme://${rest[2]}/${rest.getOrElse(3) { "" }}" + query?.let { "?$it" }.orEmpty()
                ProxyTarget.Remote(headers, upstream.toHttpUrlOrNull() ?: return null)
            }
            LOCAL -> {
                val encoded = parts[2].substringBefore('/')
                val uri = runCatching { String(Base64.getUrlDecoder().decode(encoded)) }.getOrNull() ?: return null
                ProxyTarget.Local(uri, parts[2].substringAfter('/', ""))
            }
            else -> null
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        java.security.MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

    private companion object {
        const val REMOTE = "r"
        const val LOCAL = "f"
        val UNSAFE_NAME = Regex("[^A-Za-z0-9._-]")
    }
}

internal sealed interface ProxyTarget {
    data class Remote(val headers: HeaderSet, val url: HttpUrl) : ProxyTarget

    /** [name] is the file name the URL was given, which names the format of a local playlist. */
    data class Local(val uri: String, val name: String) : ProxyTarget
}

/** Which headers a remote request carries: the stream's, or the stream's plus one external subtitle's. */
internal sealed interface HeaderSet {
    val id: String

    data object Stream : HeaderSet {
        override val id = "v"
    }

    data class Subtitle(val index: Int) : HeaderSet {
        override val id = "s$index"
    }

    companion object {
        fun of(id: String): HeaderSet? = when {
            id == Stream.id -> Stream
            id.startsWith("s") -> id.drop(1).toIntOrNull()?.takeIf { it >= 0 }?.let(::Subtitle)
            else -> null
        }
    }
}
