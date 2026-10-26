package eu.kanade.tachiyomi.extension.lnreader

/**
 * Orders versions written as dotted numbers, as LNReader plugins are. Each part is a number, so 1.10 is newer than 1.9,
 * and a missing part counts as 0, so 1.2.1 is newer than 1.2 and 1.2.0 is the same version. LNReader's own comparison
 * calls both of 1.2 and 1.2.1 newer than the other.
 */
internal fun compareDottedVersions(first: String, second: String): Int {
    val firstParts = first.split('.').map { it.toLongOrNull() ?: 0 }
    val secondParts = second.split('.').map { it.toLongOrNull() ?: 0 }
    for (index in 0 until maxOf(firstParts.size, secondParts.size)) {
        val order = firstParts.getOrElse(index) { 0 }.compareTo(secondParts.getOrElse(index) { 0 })
        if (order != 0) return order
    }
    return 0
}
