package mihon.webview.jcef.data

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/**
 * Everything Chromium keeps in its profile [directory]: caches, site storage, history and form data. It can only be
 * deleted while Chromium isn't running, which has its files open; a deletion asked for while it runs waits for the
 * next start, before Chromium opens them again.
 */
internal class ChromiumProfileData(private val directory: Path) {
    fun delete() {
        val deleted = directory.toFile().listFiles()?.all { it.deleteRecursively() } ?: true
        if (!deleted) logcat(LogPriority.WARN) { "Some of Chromium's data in $directory couldn't be deleted" }
    }

    fun deleteAtNextStart() {
        Files.createDirectories(directory)
        if (!directory.resolve(DELETE_MARKER).exists()) Files.createFile(directory.resolve(DELETE_MARKER))
    }

    /** Deletes the data if a deletion waits; called before Chromium starts. */
    fun deleteIfAsked() {
        if (!directory.resolve(DELETE_MARKER).exists()) return
        delete()
        directory.resolve(DELETE_MARKER).deleteIfExists()
    }

    private companion object {
        const val DELETE_MARKER = "katari-delete-data"
    }
}
