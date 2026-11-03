package eu.kanade.tachiyomi.data.coil

import coil3.Bitmap

internal actual fun Bitmap.iconPixels(): IconPixels {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return IconPixels(width, height, pixels)
}

internal actual fun Bitmap.croppedTo(square: IconSquare): Bitmap =
    android.graphics.Bitmap.createBitmap(this, square.x, square.y, square.side, square.side)
