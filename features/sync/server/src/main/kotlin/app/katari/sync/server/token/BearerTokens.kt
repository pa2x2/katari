package app.katari.sync.server.token

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** The secrets devices sign their calls with and browsers hold as web sessions; the server keeps only their hashes. */
internal object BearerTokens {
    private const val TOKEN_BYTES = 32

    fun generate(random: SecureRandom): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(TOKEN_BYTES).also(random::nextBytes))

    // A token is 256 random bits, so a fast hash protects it as well as a slow one and keeps every call cheap.
    fun hash(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(token.encodeToByteArray())
}
