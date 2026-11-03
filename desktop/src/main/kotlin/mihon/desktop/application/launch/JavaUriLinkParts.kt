package mihon.desktop.application.launch

import eu.kanade.tachiyomi.ui.main.link.AppLink
import java.net.URI
import java.net.URLDecoder

/** [uri] as [AppLink] reads it, decoded as Android's `Uri` decodes: a `+` is a space in the query only. */
internal class JavaUriLinkParts(private val uri: URI) : AppLink.Parts {
    override val text: String get() = uri.toString()
    override val scheme: String? get() = uri.scheme
    override val host: String? get() = uri.host
    override val path: String? get() = uri.path

    override val lastPathSegment: String?
        get() = uri.rawPath?.split('/')?.lastOrNull(String::isNotEmpty)?.let { decode(it.replace("+", "%2B")) }

    override fun queryParameter(name: String): String? = uri.rawQuery?.split('&')?.firstNotNullOfOrNull { parameter ->
        if (decode(parameter.substringBefore('=')) == name) decode(parameter.substringAfter('=', "")) else null
    }

    /** [text] with its percent escapes and pluses decoded, or null where an escape is malformed. */
    private fun decode(text: String): String? = try {
        URLDecoder.decode(text, Charsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }
}
