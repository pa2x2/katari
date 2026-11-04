package tachiyomi.core.common.storage

import mihon.core.common.platform.PlatformFacility

/** Opens a [StorageFile] again from the [StorageFile.uri] the app saved, such as in a download index or a setting. */
@PlatformFacility
interface StorageFileResolver {
    /** Null where no storage on this platform opens [uri]. */
    fun fromUri(uri: String): StorageFile?
}
