package eu.kanade.tachiyomi.data.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.ImageFetchResult
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import java.io.File

/** Icons as the package manager draws them, decoded into bitmaps here so the memory cache keeps them. */
@Inject
class AndroidExtensionIconSource(private val context: Context) : ExtensionIconSource {

    override fun packageIcon(extensionPackage: ExtensionPackage, density: ExtensionIconDensity): FetchResult {
        val appInfo = (extensionPackage as? AndroidExtensionPackage)?.packageInfo?.applicationInfo
            ?: error("Extension ${extensionPackage.packageName} isn't installed")
        val drawable: Drawable = when (density) {
            ExtensionIconDensity.System -> appInfo.loadIcon(context.packageManager)
            ExtensionIconDensity.Baseline, ExtensionIconDensity.Sharpest -> {
                val dpi = if (density == ExtensionIconDensity.Sharpest) {
                    DisplayMetrics.DENSITY_XXXHIGH
                } else {
                    DisplayMetrics.DENSITY_DEFAULT
                }
                context.packageManager.getResourcesForApplication(appInfo)
                    .getDrawableForDensity(appInfo.icon, dpi, null)
                    ?: error("Extension ${extensionPackage.packageName} has no icon")
            }
        }
        // Adaptive icons aren't bitmaps, and only bitmaps are kept in the memory cache
        return ImageFetchResult(
            image = drawable.toUnmaskedIconBitmap().asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    override fun fileIcon(file: File): FetchResult {
        val bitmap = BitmapFactory.decodeFile(file.path) ?: error("Can't decode ${file.path}")
        return ImageFetchResult(image = bitmap.asImage(), isSampled = false, dataSource = DataSource.DISK)
    }
}

/**
 * Draws an adaptive icon's layers unmasked, cropped to the part a launcher shows: the inner 72 of their 108 dp, so the
 * outer sixth on every side is left out. [ExtensionIconTrim] then crops a bitmap icon's margin.
 */
private fun Drawable.toUnmaskedIconBitmap(): Bitmap {
    if (this !is AdaptiveIconDrawable) return toBitmap()
    val size = intrinsicWidth.coerceAtLeast(1)
    val bleed = size / 4
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    listOfNotNull(background, foreground).forEach { layer ->
        layer.setBounds(-bleed, -bleed, size + bleed, size + bleed)
        layer.draw(canvas)
    }
    return bitmap
}
