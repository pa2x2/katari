package mihon.desktop.filechooser.toolkit

import java.io.File
import java.io.FilenameFilter
import java.net.URLConnection

/**
 * Hides files whose name gives a type that matches none of [mimeTypes], or null when every file matches. A file whose
 * name gives no type stays listed: backups and other app-specific formats have no registered type.
 */
internal fun mimeTypeFilenameFilter(mimeTypes: List<String>): FilenameFilter? {
    if (mimeTypes.isEmpty() || "*/*" in mimeTypes) return null
    return FilenameFilter { directory, name ->
        val type = URLConnection.guessContentTypeFromName(name)
        type == null || File(directory, name).isDirectory || mimeTypes.any { type.matchesMimeType(it) }
    }
}

private fun String.matchesMimeType(pattern: String): Boolean = if (pattern.endsWith("/*")) {
    startsWith(pattern.removeSuffix("*"), ignoreCase = true)
} else {
    equals(pattern, ignoreCase = true)
}
