// Extensions compiled against older versions call these functions in StringExtensionsKt, so every file of the
// package keeps compiling into that one class.
@file:JvmMultifileClass
@file:JvmName("StringExtensionsKt")

package eu.kanade.tachiyomi.util.lang

import org.jsoup.Jsoup

/**
 * The text of this HTML: tags dropped and entities decoded, as Android's HTML parser gives it.
 */
fun String.htmlDecode(): String {
    return Jsoup.parseBodyFragment(this).body().wholeText()
}
