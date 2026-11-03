package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import mihon.desktop.application.DesktopAppVariant
import tachiyomi.core.common.storage.FolderProvider
import java.io.File
import java.nio.file.Path

/**
 * The storage location's default: a folder named after this build of the app in the home directory, as Android's is one at the root
 * of the device's storage. Nothing is written into it unless the user picks it.
 */
@Inject
class DesktopStorageFolderProvider(private val directories: DesktopDirectories) : FolderProvider {
    private val folder: Path
        get() = directories.home.resolve(DesktopAppVariant.current.displayName)

    override fun directory(): File = folder.toFile()

    override fun path(): String = folder.toUri().toString()
}
