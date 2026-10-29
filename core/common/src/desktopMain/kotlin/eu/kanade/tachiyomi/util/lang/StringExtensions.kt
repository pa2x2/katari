// Extensions compiled against older versions call these functions in StringExtensionsKt, so every file of the
// package keeps compiling into that one class.
@file:JvmMultifileClass
@file:JvmName("StringExtensionsKt")

package eu.kanade.tachiyomi.util.lang

import org.jsoup.Jsoup
import java.nio.charset.StandardCharsets

/**
 * Returns the size of the string as the number of bytes.
 */
fun String.byteSize(): Int {
    return toByteArray(StandardCharsets.UTF_8).size
}

/**
 * Returns a string containing the first [n] bytes from this string, or the entire string if this
 * string is shorter.
 */
fun String.takeBytes(n: Int): String {
    val bytes = toByteArray(StandardCharsets.UTF_8)
    return if (bytes.size <= n) {
        this
    } else {
        bytes.decodeToString(endIndex = n).replace("�", "")
    }
}

/**
 * The text of this HTML: tags dropped and entities decoded, as Android's HTML parser gives it.
 */
fun String.htmlDecode(): String {
    return Jsoup.parseBodyFragment(this).body().wholeText()
}
