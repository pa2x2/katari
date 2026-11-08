package app.katari.desktop.storage

import app.katari.core.platform.storage.AppPrivateDirectories
import dev.zacsweers.metro.Inject
import java.io.File

/** Files the app keeps until it deletes them go to the data directory, beside the database. */
@Inject
class DesktopAppPrivateDirectories(private val directories: DesktopDirectories) : AppPrivateDirectories {
    override val cache: File
        get() = directories.cache.toFile()

    override val files: File
        get() = directories.data.toFile()

    // Nothing backs up the whole computer on the app's behalf, so these sit beside the other files it keeps.
    override val noBackupFiles: File
        get() = directories.data.toFile()
}
