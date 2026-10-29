package eu.kanade.tachiyomi.data.coil

import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Bitmap
import org.jetbrains.skia.IRect

internal actual fun Bitmap.iconPixels(): IconPixels {
    val image = asComposeImageBitmap()
    val pixels = IntArray(image.width * image.height).also { image.readPixels(it) }
    return IconPixels(image.width, image.height, pixels)
}

internal actual fun Bitmap.croppedTo(square: IconSquare): Bitmap {
    val cropped = Bitmap()
    val subset = IRect.makeXYWH(square.x, square.y, square.side, square.side)
    check(extractSubset(cropped, subset)) { "Can't crop a ${width}x$height icon to $square" }
    return cropped
}
