package eu.kanade.tachiyomi.data.coil

import app.katari.core.image.PlatformBitmap
import coil3.Image
import coil3.asImage
import coil3.request.Options

internal actual fun PlatformBitmap.toCoilImage(options: Options): Image = skia.asImage()
