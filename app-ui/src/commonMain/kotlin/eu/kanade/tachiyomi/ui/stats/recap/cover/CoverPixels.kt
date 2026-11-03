package eu.kanade.tachiyomi.ui.stats.recap.cover

import coil3.Bitmap
import coil3.request.ImageRequest

/** Decodes into memory the CPU can read, for [argbPixels]. */
internal expect fun ImageRequest.Builder.readableOnCpu(): ImageRequest.Builder

/** Every pixel row by row, as unpremultiplied ARGB ints. */
internal expect fun Bitmap.argbPixels(): IntArray
