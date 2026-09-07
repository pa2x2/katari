package mihon.entry.interactions.book.format.epub.archive

internal fun normalizeArchivePath(path: String): String {
    require(path.isNotBlank()) { "Publication archive path must not be blank" }
    require(!path.startsWith('/') && !path.startsWith('\\')) { "Publication archive path must be relative" }
    require('\\' !in path && '\u0000' !in path) { "Publication archive path contains unsafe characters" }
    val normalized = ArrayDeque<String>()
    path.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> {
                require(normalized.isNotEmpty()) { "Publication archive path escapes its container" }
                normalized.removeLast()
            }
            else -> normalized.add(segment)
        }
    }
    require(normalized.isNotEmpty()) { "Publication archive path must identify a resource" }
    return normalized.joinToString("/")
}
