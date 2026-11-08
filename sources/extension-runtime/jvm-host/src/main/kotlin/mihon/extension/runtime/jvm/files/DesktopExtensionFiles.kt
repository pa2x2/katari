package mihon.extension.runtime.jvm.files

import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import java.io.File

/**
 * The extension folders at the paths the desktop composition picks in the app's data and cache folders, each made when
 * it's asked for, as Android's app folders always exist: the cache one may have been cleared while the app ran.
 */
class DesktopExtensionFiles(
    fileExtensionRoot: File,
    privateApkDir: File,
    downloadDir: File,
) : ExtensionFiles {

    override val fileExtensionRoot: File = fileExtensionRoot
        get() = field.also { it.mkdirs() }

    override val privateApkDir: File = privateApkDir
        get() = field.also { it.mkdirs() }

    override val downloadDir: File = downloadDir
        get() = field.also { it.mkdirs() }
}
