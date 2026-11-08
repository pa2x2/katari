package tachiyomi.core.common.storage

import app.katari.core.common.platform.PlatformFacility

/**
 * The platform's index of the pictures on the device, which the app keeps its own folders out of: downloads, and a local
 * source entry's folder once it has a cover.
 */
@PlatformFacility
fun interface MediaScanner {
    fun exclude(directory: StorageFile)
}
