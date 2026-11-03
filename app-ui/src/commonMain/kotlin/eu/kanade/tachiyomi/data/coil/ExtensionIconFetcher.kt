package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.file.InstalledFileExtensionIcons
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.extension.util.ExtensionLoader

/**
 * The icon of an installed extension, drawn at [density] when it's an apk's.
 */
data class InstalledExtensionIcon(
    val pkgName: String,
    val form: InstalledForm,
    val versionCode: Long,
    val density: ExtensionIconDensity,
)

/**
 * The icon of the installed extension that provides the source with [sourceId].
 */
data class InstalledSourceIcon(val sourceId: Long)

/** An installed extension's icon: the one its store listed for a file extension, otherwise its apk's own. */
class ExtensionIconFetcher(
    private val iconSource: ExtensionIconSource,
    private val extensionLoader: Lazy<ExtensionLoader>,
    private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    private val getPkgName: () -> String?,
    private val density: ExtensionIconDensity,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val pkgName = getPkgName() ?: error("No installed extension")
        fileIcons.value.iconOf(pkgName)?.let { return iconSource.fileIcon(it) }
        val extensionPackage = extensionLoader.value.getExtensionPackageFromPkgName(pkgName)
            ?: error("Extension $pkgName isn't installed")
        return iconSource.packageIcon(extensionPackage, density)
    }

    class ExtensionFactory(
        private val iconSource: ExtensionIconSource,
        private val extensionLoader: Lazy<ExtensionLoader>,
        private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    ) : Fetcher.Factory<InstalledExtensionIcon> {
        override fun create(data: InstalledExtensionIcon, options: Options, imageLoader: ImageLoader): Fetcher {
            return ExtensionIconFetcher(iconSource, extensionLoader, fileIcons, { data.pkgName }, data.density)
        }
    }

    class SourceFactory(
        private val iconSource: ExtensionIconSource,
        private val extensionManager: Lazy<ExtensionManager>,
        private val extensionLoader: Lazy<ExtensionLoader>,
        private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    ) : Fetcher.Factory<InstalledSourceIcon> {
        override fun create(data: InstalledSourceIcon, options: Options, imageLoader: ImageLoader): Fetcher {
            return ExtensionIconFetcher(
                iconSource = iconSource,
                extensionLoader = extensionLoader,
                fileIcons = fileIcons,
                getPkgName = { extensionManager.value.getExtensionPackage(data.sourceId) },
                density = ExtensionIconDensity.System,
            )
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
        return "extension;${data.pkgName};$version;${data.density}"
    }
}

class InstalledSourceIconKeyer : Keyer<InstalledSourceIcon> {
    override fun key(data: InstalledSourceIcon, options: Options): String {
        return "source;${data.sourceId}"
    }
}
