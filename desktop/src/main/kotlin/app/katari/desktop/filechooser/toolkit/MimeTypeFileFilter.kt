package app.katari.desktop.filechooser.toolkit

import java.io.File
import java.net.URLConnection
import javax.swing.filechooser.FileFilter

/**
 * Hides files whose name gives a type that matches none of [mimeTypes], or null when every file matches. A file whose
 * name gives no type stays listed: backups and other app-specific formats have no registered type.
 */
internal fun mimeTypeFileFilter(mimeTypes: List<String>): FileFilter? {
    if (mimeTypes.isEmpty() || "*/*" in mimeTypes) return null
    return object : FileFilter() {
        override fun accept(file: File): Boolean {
            val type = URLConnection.guessContentTypeFromName(file.name)
            return type == null || file.isDirectory || mimeTypes.any { type.matchesMimeType(it) }
        }

        override fun getDescription(): String = mimeTypes.joinToString(", ")
    }
}

private fun String.matchesMimeType(pattern: String): Boolean = if (pattern.endsWith("/*")) {
    startsWith(pattern.removeSuffix("*"), ignoreCase = true)
} else {
    equals(pattern, ignoreCase = true)
}
