package eu.kanade.tachiyomi.data.coil

import coil3.Image
import coil3.asImage
import coil3.request.Options
import mihon.core.image.PlatformBitmap

internal actual fun PlatformBitmap.toCoilImage(options: Options): Image = skia.asImage()
