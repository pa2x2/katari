package eu.kanade.tachiyomi.extension.kind

import eu.kanade.tachiyomi.util.storage.copyAndSetReadOnlyTo
import java.io.File

/**
 * Returns a read-only copy of [source] in [cacheDir], named by its size and modification time so an update gets a new
 * copy, and deletes the package's older copies.
 */
internal fun cacheExtensionApk(source: File, cacheDir: File, packageName: String): File {
    val filePrefix = "$packageName-"
    val target = File(cacheDir, "$filePrefix${source.length()}-${source.lastModified()}.apk")

    if (!target.isFile || target.length() != source.length()) {
        source.copyAndSetReadOnlyTo(target, overwrite = true)
        cacheDir.listFiles()
            ?.filter { it.isFile && it != target && it.name.startsWith(filePrefix) }
            ?.forEach(File::delete)
    }

    return target
}
