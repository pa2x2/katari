package app.katari.sync.protocol.link

/**
 * What an invite's QR code holds: the server's address and the invite code, so the app opens signing up with both
 * filled in. The server's web page writes it and the app reads it; a link without either is not one.
 */
data class InviteLink(val server: String, val code: String) {
    fun encode(): String = SyncLinks.encode(KIND, listOf(SERVER to server, CODE to code))

    companion object {
        private const val KIND = "invite"
        private const val SERVER = "server"
        private const val CODE = "code"

        fun parse(text: String): InviteLink? {
            val parameters = SyncLinks.parameters(text, KIND) ?: return null
            return InviteLink(
                server = parameters[SERVER]?.takeIf(String::isNotBlank) ?: return null,
                code = parameters[CODE]?.takeIf(String::isNotBlank) ?: return null,
            )
        }
    }
}
