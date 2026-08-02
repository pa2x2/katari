package mihon.entry.interactions.manga.media

import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

internal data class MangaImmersivePageRequest(
    val imageUrl: String,
)

internal class MangaImmersivePage private constructor(
    val index: Int,
    private val sourcePage: EntryImagePage,
    private val requestResolver: MangaImmersivePageRequestResolver,
    private val loadImage: suspend (
        EntryImagePage,
        MangaImmersivePageRequest,
        (MangaImmersivePageProgress) -> Unit,
    ) -> File,
) {
    private val requestMutex = Mutex()
    private val imageMutex = Mutex()

    @Volatile
    private var resolvedRequest: MangaImmersivePageRequest? = null

    @Volatile
    private var imageFile: File? = null

    private val mutableProgress = MutableStateFlow(MangaImmersivePageProgress())
    val progress: StateFlow<MangaImmersivePageProgress> = mutableProgress

    suspend fun resolveRequest(): MangaImmersivePageRequest {
        resolvedRequest?.let { return it }
        return requestMutex.withLock {
            resolvedRequest ?: requestResolver.resolve(sourcePage)
                .also { resolvedRequest = it }
        }
    }

    suspend fun loadImage(): File {
        imageFile?.takeIf(File::exists)?.let { return it }
        return imageMutex.withLock {
            imageFile?.takeIf(File::exists) ?: loadImage(
                sourcePage,
                resolveRequest(),
                { mutableProgress.value = it },
            ).also { imageFile = it }
        }
    }

    companion object {
        fun unresolved(
            index: Int,
            sourcePage: EntryImagePage,
            requestResolver: MangaImmersivePageRequestResolver,
            loadImage: suspend (
                EntryImagePage,
                MangaImmersivePageRequest,
                (MangaImmersivePageProgress) -> Unit,
            ) -> File,
        ): MangaImmersivePage {
            return MangaImmersivePage(
                index = index,
                sourcePage = sourcePage,
                requestResolver = requestResolver,
                loadImage = loadImage,
            )
        }
    }
}

internal data class MangaImmersivePageProgress(
    val bytesRead: Long = 0L,
    val contentLength: Long = -1L,
) {
    val fraction: Float?
        get() = contentLength.takeIf { it > 0L }
            ?.let { (bytesRead.toFloat() / it).coerceIn(0f, 1f) }
}

internal class MangaImmersivePageRequestResolver(
    private val imageSource: EntryImageSource,
) {
    suspend fun resolve(page: EntryImagePage): MangaImmersivePageRequest {
        val imageUrl = page.imageUrl
            ?.takeIf(String::isNotBlank)
            ?: imageSource.getImageUrl(page)
        require(imageUrl.isNotBlank()) { "No image URL found for page ${page.index + 1}" }
        return MangaImmersivePageRequest(imageUrl)
    }
}
