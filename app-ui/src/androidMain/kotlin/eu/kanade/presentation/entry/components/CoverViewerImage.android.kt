package eu.kanade.presentation.entry.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updatePadding
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.github.chrisbanes.photoview.PhotoView
import tachiyomi.presentation.core.util.clickableNoIndication

// Pinching and double-tapping zoom through PhotoView.
@Composable
internal actual fun CoverViewerImage(data: Any?, contentPadding: PaddingValues, onClick: () -> Unit) {
    val statusBarPaddingPx = with(LocalDensity.current) { contentPadding.calculateTopPadding().roundToPx() }
    val bottomPaddingPx = with(LocalDensity.current) { contentPadding.calculateBottomPadding().roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickableNoIndication(onClick = onClick),
    ) {
        AndroidView(
            factory = {
                PhotoView(it).apply {
                    setOnClickListener { onClick() }
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
            },
            update = { view ->
                if (view.tag != data) {
                    view.tag = data
                    val request = ImageRequest.Builder(view.context)
                        .data(data)
                        .size(Size.ORIGINAL)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .target { image ->
                            val drawable = image.asDrawable(view.context.resources)
                            // Copy bitmap in case it came from memory cache
                            // Because SSIV needs to thoroughly read the image
                            val copy = (drawable as? BitmapDrawable)
                                ?.bitmap
                                ?.copy(Bitmap.Config.HARDWARE, false)
                                ?.toDrawable(view.context.resources)
                                ?: drawable
                            view.setImageDrawable(copy)
                        }
                        .build()
                    view.context.imageLoader.enqueue(request)
                }

                view.updatePadding(top = statusBarPaddingPx, bottom = bottomPaddingPx)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
