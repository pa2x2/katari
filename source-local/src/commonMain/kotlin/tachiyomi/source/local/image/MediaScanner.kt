package tachiyomi.source.local.image

import tachiyomi.core.common.storage.StorageFile

/** The platform's index of the pictures on the device, which an entry's folder is kept out of once it has a cover. */
fun interface MediaScanner {
    fun exclude(directory: StorageFile)
}
