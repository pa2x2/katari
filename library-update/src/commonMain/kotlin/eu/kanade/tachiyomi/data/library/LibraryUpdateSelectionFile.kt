package eu.kanade.tachiyomi.data.library

import dev.zacsweers.metro.Inject
import mihon.core.platform.storage.AppPrivateDirectories
import java.io.File
import java.util.UUID

/**
 * Hands the entries picked for an update to the update. Android's WorkManager caps an update's input at 10 KB, which a
 * selection in a large library can exceed, so the ids travel in a file the update's input names.
 */
@Inject
class LibraryUpdateSelectionFile(private val directories: AppPrivateDirectories) {

    private val directory: File
        get() = File(directories.noBackupFiles, "library_update_selection")

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
