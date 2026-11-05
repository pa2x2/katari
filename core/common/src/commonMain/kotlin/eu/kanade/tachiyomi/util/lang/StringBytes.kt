// Extensions compiled against older versions call these functions in StringExtensionsKt, so every file of the
// package keeps compiling into that one class.
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringExtensionsKt")

package eu.kanade.tachiyomi.util.lang

/** The length of the string in UTF-8 bytes. */
fun String.byteSize(): Int = encodeToByteArray().size

/**
 * Returns a string containing the first [n] bytes from this string, or the entire string if this
 * string is shorter.
 */
fun String.takeBytes(n: Int): String {
    val bytes = encodeToByteArray()
    return if (bytes.size <= n) {
        this
    } else {
        bytes.decodeToString(endIndex = n).replace("�", "")
    }
}
