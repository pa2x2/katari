package mihon.extension.runtime.jvm.files

import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import java.io.File

/** The extension folders at the paths the desktop composition picks in the app's data and cache folders. */
class DesktopExtensionFiles(
    override val fileExtensionRoot: File,
    override val privateApkDir: File,
    override val downloadDir: File,
) : ExtensionFiles
