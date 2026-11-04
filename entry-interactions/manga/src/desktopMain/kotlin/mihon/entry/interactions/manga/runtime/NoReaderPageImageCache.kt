package mihon.entry.interactions.manga.runtime

import mihon.entry.interactions.runtime.EntryPageImageCache
import java.io.File

internal object NoReaderPageImageCache : EntryPageImageCache {
    override val readableSize: String = "0 B"

    override fun isImageInCache(imageUrl: String): Boolean = false

    override fun getImageFile(imageUrl: String): File = throw NoSuchElementException("No page is cached: $imageUrl")

    override fun clear(): Int = 0
}
