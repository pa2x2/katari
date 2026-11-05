package mihon.core.image

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decoded 8-bit RGBA pixels with premultiplied alpha, in the platform's own bitmap type: what the image decoder writes
 * into and what each platform draws or hands on without a copy.
 */
expect class PlatformBitmap

expect val PlatformBitmap.pixelWidth: Int

expect val PlatformBitmap.pixelHeight: Int

/** The memory the pixels take. */
expect val PlatformBitmap.byteCount: Long

/** The colour of the pixel at [x], [y] as ARGB without premultiplied alpha. */
expect fun PlatformBitmap.argbPixel(x: Int, y: Int): Int

/** Frees the pixels at once rather than when the bitmap is collected; the bitmap is not used afterwards. */
expect fun PlatformBitmap.release()

/** The same pixels as Compose draws them, without a copy; valid until the bitmap is released. */
expect fun PlatformBitmap.asImageBitmap(): ImageBitmap
