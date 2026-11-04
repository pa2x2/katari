package tachiyomi.core.common.storage

/** Desktops keep no picture index that the app's images would show up in. */
object NoMediaScanner : MediaScanner {
    override fun exclude(directory: StorageFile) = Unit
}
