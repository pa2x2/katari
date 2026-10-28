package eu.kanade.presentation.more.settings.screen.data

import java.io.File

/** The storage the app keeps its files on, which the data settings show the free space of. */
interface StorageVolumes {
    /**
     * A folder on each storage, in the order the settings list them. Reads the disks, so call it off the main thread.
     */
    fun roots(): List<File>

    /** The free and total space of the storage [root] is on. Reads the disk, so call it off the main thread. */
    fun space(root: File): StorageSpace
}

data class StorageSpace(val availableBytes: Long, val totalBytes: Long)
