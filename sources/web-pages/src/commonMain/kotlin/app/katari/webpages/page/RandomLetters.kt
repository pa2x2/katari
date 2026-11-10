package app.katari.webpages.page

import java.security.SecureRandom

private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"

/** A name or mark a page's own scripts can't guess. */
internal fun randomLetters(count: Int): String {
    val random = SecureRandom()
    return buildString { repeat(count) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
}
