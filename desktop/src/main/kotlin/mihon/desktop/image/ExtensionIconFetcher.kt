package mihon.desktop.image

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.file.InstalledFileExtensionIcons
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import mihon.extension.runtime.jvm.icons.ApkIcons
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath

/** The icon of an installed extension: an apk's own, or the one a file extension's store listed. */
data class InstalledExtensionIcon(val pkgName: String, val form: InstalledForm, val versionCode: Long)

/** The icon of the installed extension that provides the source with [sourceId]. */
data class InstalledSourceIcon(val sourceId: Long)

/** Fails when the extension has no icon, which shows the row's fallback. */
class ExtensionIconFetcher(
    private val extensionLoader: Lazy<ExtensionLoader>,
    private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    private val getPkgName: () -> String?,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val pkgName = getPkgName() ?: error("No installed extension")
        fileIcons.value.iconOf(pkgName)?.let { icon ->
            return SourceFetchResult(
                source = ImageSource(icon.toOkioPath(), FileSystem.SYSTEM),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }
        val extensionPackage = extensionLoader.value.getExtensionPackageFromPkgName(pkgName)
            ?: error("Extension $pkgName isn't installed")
        val icon = ApkIcons.iconOf(extensionPackage) ?: error("Extension $pkgName has no bitmap icon")
        return SourceFetchResult(
            source = ImageSource(Buffer().write(icon), FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class ExtensionFactory(
        private val extensionLoader: Lazy<ExtensionLoader>,
        private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    ) : Fetcher.Factory<InstalledExtensionIcon> {
        override fun create(data: InstalledExtensionIcon, options: Options, imageLoader: ImageLoader): Fetcher =
            ExtensionIconFetcher(extensionLoader, fileIcons) { data.pkgName }
    }

    class SourceFactory(
        private val extensionManager: Lazy<ExtensionManager>,
        private val extensionLoader: Lazy<ExtensionLoader>,
        private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    ) : Fetcher.Factory<InstalledSourceIcon> {
        override fun create(data: InstalledSourceIcon, options: Options, imageLoader: ImageLoader): Fetcher =
            ExtensionIconFetcher(extensionLoader, fileIcons) {
                extensionManager.value.getExtensionPackage(data.sourceId)
            }
    }
}

class InstalledExtensionIconKeyer : Keyer<InstalledExtensionIcon> {
    override fun key(data: InstalledExtensionIcon, options: Options): String {
        // A file's version code stays 0, so its code's hash tells its versions apart
        val version = when (val form = data.form) {
            is InstalledForm.Package -> data.versionCode.toString()
            is InstalledForm.File -> form.sha256
        }
        return "extension;${data.pkgName};$version"
    }
}

class InstalledSourceIconKeyer : Keyer<InstalledSourceIcon> {
    override fun key(data: InstalledSourceIcon, options: Options): String = "source;${data.sourceId}"
}
