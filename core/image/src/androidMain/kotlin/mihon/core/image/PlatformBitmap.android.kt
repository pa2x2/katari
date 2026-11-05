package mihon.core.image

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap as asComposeImageBitmap

actual typealias PlatformBitmap = Bitmap

actual val PlatformBitmap.pixelWidth: Int get() = width

actual val PlatformBitmap.pixelHeight: Int get() = height

actual val PlatformBitmap.byteCount: Long get() = allocationByteCount.toLong()

actual fun PlatformBitmap.release() = recycle()

actual fun PlatformBitmap.asImageBitmap(): ImageBitmap = asComposeImageBitmap()
