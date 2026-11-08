package mihon.extension.runtime.jvm.preferences

import logcat.LogPriority
import mihon.extension.compat.SharedPreferencesFiles
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * A folder of [FileSharedPreferences], `<name>.json` for each name, as Android keeps an app's shared preferences in
 * one folder. Each name opens one instance for the whole run, which stays after [delete] and starts empty, so a
 * file has one writer however often it's opened.
 */
class SharedPreferencesDirectory(
    private val directory: Path,
    private val listenerThread: Executor,
) : SharedPreferencesFiles {

    private val opened = ConcurrentHashMap<String, FileSharedPreferences>()

    override fun open(name: String): FileSharedPreferences = opened.computeIfAbsent(name) {
        FileSharedPreferences(DesktopPreferenceStore(path(name)), listenerThread)
    }

    override fun delete(name: String): Boolean {
        val path = path(name)
        try {
            val preferences = opened[name]
            if (preferences != null) preferences.store.clear() else Files.deleteIfExists(path)
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not delete preferences at $path" }
        }
        return !Files.exists(path)
    }

    /** The names of the files in the folder, and of those opened this run, whose changes may not be written yet. */
    fun names(): Sequence<String> {
        val written = if (Files.isDirectory(directory)) {
            directory.listDirectoryEntries("*$EXTENSION")
                .filter { it.isRegularFile() }
                .map { it.name.removeSuffix(EXTENSION) }
        } else {
            emptyList()
        }
        return (written + opened.keys).distinct().asSequence()
    }

    private fun path(name: String): Path = directory.resolve("$name$EXTENSION")

    private companion object {
        const val EXTENSION = ".json"
    }
}
