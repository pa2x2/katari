package app.katari.sync.client.account

import app.katari.sync.protocol.link.SyncLinks
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * What a link code's QR code holds (D024, D027): the server's address and the code, which are all a new device needs,
 * and the account's name and the showing device's name, so the new device can say whose account it is joining before
 * it does. Typed codes carry only the code; the person types the address beside it. A link without a server or a code
 * is not one.
 */
data class LinkCodeLink(val server: HttpUrl, val code: String, val accountName: String?, val fromDevice: String?) {
    fun encode(): String = SyncLinks.encode(
        KIND,
        listOfNotNull(
            SERVER to server.toString(),
            CODE to code,
            accountName?.let { ACCOUNT to it },
            fromDevice?.let { FROM to it },
        ),
    )

    companion object {
        fun parse(text: String): LinkCodeLink? {
            val parameters = SyncLinks.parameters(text, KIND) ?: return null
            return LinkCodeLink(
                server = parameters[SERVER]?.toHttpUrlOrNull() ?: return null,
                code = parameters[CODE]?.takeIf { it.isNotBlank() } ?: return null,
                accountName = parameters[ACCOUNT]?.takeIf { it.isNotBlank() },
                fromDevice = parameters[FROM]?.takeIf { it.isNotBlank() },
            )
        }

        private const val KIND = "link"
        private const val SERVER = "server"
        private const val CODE = "code"
        private const val ACCOUNT = "account"
        private const val FROM = "from"
    }
}
