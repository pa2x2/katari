package mihon.desktop.image

import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.coil.ExtensionIconDensity
import eu.kanade.tachiyomi.data.coil.ExtensionIconSource
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import mihon.extension.runtime.jvm.icons.ApkIcons
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * Icons read from the apk's resources, which the image loader decodes. An apk holds its icon at several densities and
 * the sharpest is always read, so every [ExtensionIconDensity] draws the same one; an icon only drawn as XML fails.
 */
@Inject
class ApkExtensionIconSource : ExtensionIconSource {

    override fun packageIcon(extensionPackage: ExtensionPackage, density: ExtensionIconDensity): FetchResult {
        val icon = ApkIcons.iconOf(extensionPackage)
            ?: error("Extension ${extensionPackage.packageName} has no bitmap icon")
        return SourceFetchResult(
            source = ImageSource(Buffer().write(icon), FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    override fun fileIcon(file: File): FetchResult = SourceFetchResult(
        source = ImageSource(file.toOkioPath(), FileSystem.SYSTEM),
        mimeType = null,
        dataSource = DataSource.DISK,
    )
}
