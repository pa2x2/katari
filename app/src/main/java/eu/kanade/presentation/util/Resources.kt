package eu.kanade.presentation.util

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Create a BitmapPainter from a drawable resource.
 * Use this only if [androidx.compose.ui.res.painterResource] doesn't work.
 *
 * The drawable is rasterized once per resource and configuration, because list items such as covers request it for
 * every composed row and inflating and drawing it each time stalls scrolling.
 *
 * @param id the resource identifier
 *
 * @return the bitmap associated with the resource
 */
@Composable
fun rememberResourceBitmapPainter(@DrawableRes id: Int): BitmapPainter {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val key = ResourceBitmapKey(id, configuration.uiMode, configuration.densityDpi)
    return remember(key) {
        val bitmap = resourceBitmaps.getOrPut(key) {
            val drawable = ContextCompat.getDrawable(context, id)
                ?: throw Resources.NotFoundException()
            drawable.toBitmap().asImageBitmap()
        }
        BitmapPainter(bitmap)
    }
}

private data class ResourceBitmapKey(
    val id: Int,
    val uiMode: Int,
    val densityDpi: Int,
)

private val resourceBitmaps = ConcurrentHashMap<ResourceBitmapKey, ImageBitmap>()
