package mihon.entry.interactions.manga.page.acquisition

import kotlinx.coroutines.flow.StateFlow
import mihon.core.common.image.progressive.ProgressiveImageDecodeOptions
import mihon.core.common.image.progressive.ProgressiveImageState
import mihon.entry.interactions.manga.page.MangaPageStore
import okhttp3.Response
import java.io.File

internal class StoredMangaPageAcquirer(
    private val store: MangaPageStore,
) : MangaPageAcquirer {
    override suspend fun acquire(
        imageUrl: String,
        force: Boolean,
        options: ProgressiveImageDecodeOptions,
        onFetch: () -> Unit,
        onProgressiveState: (StateFlow<ProgressiveImageState>?) -> Unit,
        fetch: suspend () -> Response,
    ): File {
        onProgressiveState(null)
        return store.getOrPutImage(imageUrl, force, onFetch, fetch)
    }
}
