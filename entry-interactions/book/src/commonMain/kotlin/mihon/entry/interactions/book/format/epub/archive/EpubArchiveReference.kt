package mihon.entry.interactions.book.format.epub.archive

import java.net.URI

internal fun resolveArchiveReference(baseResource: String, reference: String): EpubArchiveReference? {
    val trimmed = reference.trim()
    if (trimmed.isEmpty()) return null
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    if (uri.isAbsolute) return EpubArchiveReference.External(trimmed)
    val fragment = uri.fragment?.takeIf(String::isNotBlank)
    val path = uri.path.orEmpty()
    val resolved = if (path.isEmpty()) {
        normalizeArchivePath(baseResource)
    } else {
        val baseDirectory = normalizeArchivePath(baseResource).substringBeforeLast('/', "")
        normalizeArchivePath(
            listOf(baseDirectory, path).filter(String::isNotEmpty).joinToString("/"),
        )
    }
    return EpubArchiveReference.Internal(resolved, fragment)
}

internal sealed interface EpubArchiveReference {
    data class Internal(val path: String, val fragment: String?) : EpubArchiveReference
    data class External(val url: String) : EpubArchiveReference
}
