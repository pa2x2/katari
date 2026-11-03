package tachiyomi.source.local.image

import mihon.core.common.platform.PlatformFacility
import tachiyomi.core.common.storage.StorageFile

/** The platform's index of the pictures on the device, which an entry's folder is kept out of once it has a cover. */
@PlatformFacility
fun interface MediaScanner {
    fun exclude(directory: StorageFile)
}
