package eu.kanade.tachiyomi.extension.platform

import mihon.core.common.platform.PlatformFacility
import java.io.File

/** The folders extensions are kept in, which survive until the app is uninstalled, apart from [downloadDir]. */
@PlatformFacility
interface ExtensionFiles {

    /** Where file extensions are installed, laid out as `FileExtensionStorage` describes. */
    val fileExtensionRoot: File

    /** Where the app's private copies of apks are kept. */
    val privateApkDir: File

    /** Where apks are downloaded to before they're installed; the platform may clear it. */
    val downloadDir: File
}
