package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader

/** The image loader the application hands Coil when it asks for one ([AppImageLoaderBindings]). */
interface ImageLoaderGraph {
    val imageLoader: ImageLoader
}
