package eu.kanade.tachiyomi.data.library

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Hands the entries picked for an update to [LibraryUpdateJob]. WorkManager input is capped at 10 KB, which a selection
 * in a large library can exceed, so the ids travel in a file the job's input names.
 */
internal class LibraryUpdateSelectionFile(context: Context) {

    private val directory = File(context.noBackupFilesDir, "library_update_selection")

    fun write(entryIds: Collection<Long>): String {
        directory.mkdirs()
        val name = UUID.randomUUID().toString()
        File(directory, name).writeText(entryIds.joinToString(","))
        return name
    }

    /** Empty once the file is gone, such as after the update that used it finished. */
    fun read(name: String): Set<Long> {
        val file = File(directory, name)
        if (!file.exists()) return emptySet()
        return file.readText().split(',').mapNotNull(String::toLongOrNull).toSet()
    }

    fun delete(name: String) {
        File(directory, name).delete()
    }
}
