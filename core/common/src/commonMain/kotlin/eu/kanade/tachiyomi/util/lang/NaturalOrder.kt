// Extensions compiled against older versions call these functions in StringExtensionsKt, so every file of the
// package keeps compiling into that one class.
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringExtensionsKt")

package eu.kanade.tachiyomi.util.lang

import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator

/**
 * Case-insensitive natural comparator for strings.
 */
fun String.compareToCaseInsensitiveNaturalOrder(other: String): Int {
    val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()
    return comparator.compare(this, other)
}
