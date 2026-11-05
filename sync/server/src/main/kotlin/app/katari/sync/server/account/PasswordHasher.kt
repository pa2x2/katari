package app.katari.sync.server.account

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Argon2id password hashes in the PHC string form, `$argon2id$v=19$m=<KiB>,t=<passes>,p=<lanes>$<salt>$<hash>` with
 * unpadded Base64. Each hash carries its own cost, so raising [cost] later leaves existing hashes verifiable.
 */
class PasswordHasher(
    private val cost: Argon2Cost = Argon2Cost.DEFAULT,
    private val random: SecureRandom = SecureRandom(),
) {
    // Verified against when a username is unknown, so the answer takes as long as for a wrong password.
    private val decoy by lazy { hash(DECOY_PASSWORD) }

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(password, salt, cost, HASH_BYTES)
        return "\$argon2id\$v=19\$m=${cost.memoryKib},t=${cost.iterations},p=${cost.parallelism}" +
            "\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(hash)}"
    }

    /** False for a wrong password and for a [hash] that is not in the form [hash] writes. */
    fun verify(password: String, hash: String): Boolean {
        val parts = hash.split('$')
        if (parts.size != 6 || parts[0].isNotEmpty() || parts[1] != "argon2id" || parts[2] != "v=19") return false
        val parameters = parts[3].split(',').associate {
            it.substringBefore('=') to it.substringAfter('=').toIntOrNull()
        }
        val cost = Argon2Cost(
            memoryKib = parameters["m"] ?: return false,
            iterations = parameters["t"] ?: return false,
            parallelism = parameters["p"] ?: return false,
        )
        val salt = runCatching { decoder.decode(parts[4]) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(parts[5]) }.getOrNull() ?: return false
        return MessageDigest.isEqual(derive(password, salt, cost, expected.size), expected)
    }

    /** Spends the time [verify] would, for a sign-in whose account does not exist. */
    fun verifyNothing(password: String) {
        verify(password, decoy)
    }

    private fun derive(password: String, salt: ByteArray, cost: Argon2Cost, length: Int): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(cost.memoryKib)
            .withIterations(cost.iterations)
            .withParallelism(cost.parallelism)
            .withSalt(salt)
            .build()
        val generator = Argon2BytesGenerator().apply { init(parameters) }
        return ByteArray(length).also { generator.generateBytes(password.encodeToByteArray(), it) }
    }

    private companion object {
        const val SALT_BYTES = 16
        const val HASH_BYTES = 32
        const val DECOY_PASSWORD = "no account has this password"
        val encoder: Base64.Encoder = Base64.getEncoder().withoutPadding()
        val decoder: Base64.Decoder = Base64.getDecoder()
    }
}

data class Argon2Cost(
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
) {
    companion object {
        /** OWASP's recommended minimum for Argon2id, which stays quick on the small machines such a server runs on. */
        val DEFAULT = Argon2Cost(memoryKib = 19 * 1024, iterations = 2, parallelism = 1)
    }
}
