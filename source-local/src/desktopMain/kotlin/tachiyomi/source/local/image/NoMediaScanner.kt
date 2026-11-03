package tachiyomi.source.local.image

import tachiyomi.core.common.storage.StorageFile

/** Desktops keep no picture index that covers would show up in. */
object NoMediaScanner : MediaScanner {
    override fun exclude(directory: StorageFile) = Unit
}
