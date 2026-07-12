package eu.kanade.tachiyomi.util.lang

fun String.toStoredDisplayName(sourceTitle: String): String? {
    val trimmed = trim()
    return trimmed
        .takeUnless { it.isBlank() || it == sourceTitle }
}
