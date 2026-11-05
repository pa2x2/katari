// Extensions compiled against older versions call these functions in StringExtensionsKt, so every file of the
// package keeps compiling into that one class.
@file:JvmMultifileClass
@file:JvmName("StringExtensionsKt")

package eu.kanade.tachiyomi.util.lang

import androidx.core.text.parseAsHtml

/**
 * HTML-decode the string
 */
fun String.htmlDecode(): String {
    return this.parseAsHtml().toString()
}
