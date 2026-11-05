package eu.kanade.tachiyomi.data.coil

import coil3.Image
import coil3.request.Options
import mihon.core.image.PlatformBitmap

/** The decoded [PlatformBitmap] as the image Coil hands out, in the bitmap kind [options] ask for where that differs. */
internal expect fun PlatformBitmap.toCoilImage(options: Options): Image
