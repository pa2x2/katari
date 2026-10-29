package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import mihon.core.platform.storage.AppPrivateDirectories
import java.io.File

/** Files the app keeps until it deletes them go to the data directory, beside the database. */
@Inject
class DesktopAppPrivateDirectories(private val directories: DesktopDirectories) : AppPrivateDirectories {
    override val cache: File
        get() = directories.cache.toFile()

    override val files: File
        get() = directories.data.toFile()
}
