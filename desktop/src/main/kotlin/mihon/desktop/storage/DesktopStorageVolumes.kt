package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.data.StorageSpace
import eu.kanade.presentation.more.settings.screen.data.StorageVolumes
import tachiyomi.domain.storage.service.StoragePreferences
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/** The disk holding the app's data, then the one holding the storage location where that is another. */
@Inject
class DesktopStorageVolumes(
    private val directories: DesktopDirectories,
    private val storagePreferences: StoragePreferences,
) : StorageVolumes {
    override fun roots(): List<File> {
        val storageLocation = storagePreferences.baseStorageDirectory.get().toFolderPath()
            ?.takeIf { Files.isDirectory(it) }
        return listOfNotNull(directories.data, storageLocation)
            .distinctBy { Files.getFileStore(it) }
            .map(Path::toFile)
    }

    override fun space(root: File): StorageSpace =
        StorageSpace(availableBytes = root.usableSpace, totalBytes = root.totalSpace)
}
