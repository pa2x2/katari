package app.katari.sync.client.account

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * What a link code's QR code holds (D024, D027): the server's address and the code, which are all a new device needs,
 * and the account's name and the showing device's name, so the new device can say whose account it is joining before
 * it does. It is a `katari-sync:` link, so a camera app that isn't Katari's shows it as one rather than as plain text.
 * Typed codes carry only the code; the person types the address beside it.
 *
 * Later versions may add parameters; a reader ignores the ones it doesn't know, and a code without a server or a code
 * is not one.
 */
data class LinkCodeLink(val server: HttpUrl, val code: String, val accountName: String?, val fromDevice: String?) {
    fun encode(): String = HttpUrl.Builder()
        .scheme("https")
        .host(PLACEHOLDER_HOST)
        .addQueryParameter(SERVER, server.toString())
        .addQueryParameter(CODE, code)
        .apply {
            accountName?.let { addQueryParameter(ACCOUNT, it) }
            fromDevice?.let { addQueryParameter(FROM, it) }
        }
        .build()
        .encodedQuery
        .let { "$SCHEME:link?$it" }

    companion object {
        fun parse(text: String): LinkCodeLink? {
            val prefix = "$SCHEME:link?"
            if (!text.startsWith(prefix)) return null
            // Read through HttpUrl, which decodes the query as it was encoded.
            val query = "https://$PLACEHOLDER_HOST/?${text.removePrefix(prefix)}".toHttpUrlOrNull() ?: return null
            val server = query.queryParameter(SERVER)?.toHttpUrlOrNull() ?: return null
            val code = query.queryParameter(CODE)?.takeIf { it.isNotBlank() } ?: return null
            return LinkCodeLink(
                server = server,
                code = code,
                accountName = query.queryParameter(ACCOUNT)?.takeIf { it.isNotBlank() },
                fromDevice = query.queryParameter(FROM)?.takeIf { it.isNotBlank() },
            )
        }

        private const val SCHEME = "katari-sync"
        private const val PLACEHOLDER_HOST = "link.invalid"
        private const val SERVER = "server"
        private const val CODE = "code"
        private const val ACCOUNT = "account"
        private const val FROM = "from"
    }
}
