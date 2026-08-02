package mihon.entry.interactions.manga.media

import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class MangaImmersivePageLoader(
    private val imageSource: EntryImageSource,
    private val pageCache: Lazy<ReaderPageCache>,
) {
    suspend fun load(
        page: EntryImagePage,
        request: MangaImmersivePageRequest,
        onProgress: (MangaImmersivePageProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val cache = pageCache.value
        if (cache.isImageInCache(request.imageUrl)) {
            onProgress(MangaImmersivePageProgress(bytesRead = 1L, contentLength = 1L))
            return@withContext cache.getImageFile(request.imageUrl)
        }

        onProgress(MangaImmersivePageProgress())
        val resolvedPage = page.copy(imageUrl = request.imageUrl)
        val response = imageSource.getImage(
            page = resolvedPage,
            progress = object : ProgressListener {
                override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                    onProgress(MangaImmersivePageProgress(bytesRead, contentLength))
                }
            },
        )
        cache.putImageToCacheCancellable(request.imageUrl, response)
        onProgress(MangaImmersivePageProgress(bytesRead = 1L, contentLength = 1L))
        cache.getImageFile(request.imageUrl)
    }
}
