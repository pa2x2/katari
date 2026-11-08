package app.katari.desktop.image

import app.katari.desktop.storage.DesktopDirectories
import eu.kanade.tachiyomi.data.cache.CoverCacheDirectories
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Downloaded covers in the cache directory, which the system or the user may clear since they can be downloaded again,
 * and the covers users set in the data directory, since nothing could bring them back.
 */
class DesktopCoverCacheDirectories(private val directories: DesktopDirectories) : CoverCacheDirectories {
    override val covers: File
        get() = existing(directories.cache.resolve("covers"))

    override val customCovers: File
        get() = existing(directories.data.resolve("covers/custom"))

    private fun existing(directory: Path): File = Files.createDirectories(directory).toFile()
}
