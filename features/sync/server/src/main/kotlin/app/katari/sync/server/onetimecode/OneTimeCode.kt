package app.katari.sync.server.onetimecode

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Codes a person reads off a screen or a log and types in: Crockford Base32 in groups of four, read back forgivingly,
 * so case, dashes and spaces don't matter and the letters O, I and L count as the digits they look like.
 */
internal object OneTimeCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun generate(length: Int, random: SecureRandom): String =
        CharArray(length) { ALPHABET[random.nextInt(ALPHABET.length)] }
            .concatToString()
            .chunked(4)
            .joinToString("-")

    /** The SHA-256 of the code as typed, read back forgivingly; two ways of typing the same code hash the same. */
    fun hash(typed: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(canonical(typed).encodeToByteArray())

    fun matches(typed: String, code: String): Boolean = MessageDigest.isEqual(hash(typed), hash(code))

    private fun canonical(typed: String): String = typed.uppercase()
        .filterNot { it == '-' || it.isWhitespace() }
        .map {
            when (it) {
                'O' -> '0'
                'I', 'L' -> '1'
                else -> it
            }
        }
        .joinToString("")
}
