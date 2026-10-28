package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.FolderProvider
import java.io.File

/** Until the user picks a storage location, the app's data directory stands for it. */
@Inject
class DesktopStorageFolderProvider(private val directories: DesktopDirectories) : FolderProvider {
    override fun directory(): File = directories.data.toFile()

    override fun path(): String = directories.data.toUri().toString()
}
