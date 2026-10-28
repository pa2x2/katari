package eu.kanade.tachiyomi.data.coil

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.file.InstalledFileExtensionIcons
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionPackage
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import java.io.File

/**
 * The icon of an installed extension, drawn at [density] when it's an apk's.
 */
data class InstalledExtensionIcon(
    val pkgName: String,
    val form: InstalledForm,
    val versionCode: Long,
    val density: Int,
)

/**
 * The icon of the installed extension that provides the source with [sourceId].
 */
data class InstalledSourceIcon(val sourceId: Long)

class ExtensionIconFetcher(
    private val context: Context,
    private val extensionLoader: Lazy<ExtensionLoader>,
    private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    private val getPkgName: () -> String?,
    private val density: Int?,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val pkgName = getPkgName() ?: error("No installed extension")
        fileIcons.value.iconOf(pkgName)?.let { return fetchFileIcon(it) }
        val extensionPackage = extensionLoader.value.getExtensionPackageFromPkgName(pkgName) as? AndroidExtensionPackage
        val appInfo = extensionPackage?.packageInfo?.applicationInfo
            ?: error("Extension $pkgName isn't installed")
        val drawable: Drawable = if (density == null) {
            appInfo.loadIcon(context.packageManager)
        } else {
            context.packageManager.getResourcesForApplication(appInfo)
                .getDrawableForDensity(appInfo.icon, density, null)
                ?: error("Extension $pkgName has no icon")
        }
        // Adaptive icons aren't bitmaps, and only bitmaps are kept in the memory cache
        return ImageFetchResult(
            image = drawable.toUnmaskedIconBitmap().asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    private fun fetchFileIcon(icon: File): FetchResult {
        val bitmap = BitmapFactory.decodeFile(icon.path) ?: error("Can't decode ${icon.path}")
        return ImageFetchResult(image = bitmap.asImage(), isSampled = false, dataSource = DataSource.DISK)
    }

    class ExtensionFactory(
        private val context: Context,
        private val extensionLoader: Lazy<ExtensionLoader>,
        private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    ) : Fetcher.Factory<InstalledExtensionIcon> {
        override fun create(data: InstalledExtensionIcon, options: Options, imageLoader: ImageLoader): Fetcher {
            return ExtensionIconFetcher(context, extensionLoader, fileIcons, { data.pkgName }, data.density)
        }
    }

    class SourceFactory(
        private val context: Context,
        private val extensionManager: Lazy<ExtensionManager>,
        private val extensionLoader: Lazy<ExtensionLoader>,
        private val fileIcons: Lazy<InstalledFileExtensionIcons>,
    ) : Fetcher.Factory<InstalledSourceIcon> {
        override fun create(data: InstalledSourceIcon, options: Options, imageLoader: ImageLoader): Fetcher {
            return ExtensionIconFetcher(
                context = context,
                extensionLoader = extensionLoader,
                fileIcons = fileIcons,
                getPkgName = { extensionManager.value.getExtensionPackage(data.sourceId) },
                density = null,
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
