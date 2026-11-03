package eu.kanade.tachiyomi.extension.file

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import java.io.File

/** The icons file extensions were installed with, which their stores listed. */
@Inject
class InstalledFileExtensionIcons(
    private val kinds: ExtensionKinds,
    private val files: ExtensionFiles,
) {

    /** The icon of the file extension [pkgName], `null` when [pkgName] is an apk's. Throws when it isn't installed. */
    fun iconOf(pkgName: String): File? {
        val (kind, id) = kinds.ofFilePkgName(pkgName) ?: return null
        val stored = FileExtensionStorage(files).readInstalled(kind, id) as? StoredFileExtension.Readable
            ?: error("Extension ${kind.directoryName}:$id isn't installed")
        return stored.extension.icon
    }
}
