package app.katari.entry.interactions.manga.page.acquisition

import app.katari.core.image.progressive.ProgressiveImageDecodeOptions
import app.katari.core.image.progressive.ProgressiveImageState
import app.katari.entry.interactions.manga.page.MangaPageStore
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Response
import java.io.File

internal class StoredMangaPageAcquirer(
    private val store: MangaPageStore,
) : MangaPageAcquirer {
    override val prioritizesVisiblePages = false

    override suspend fun acquire(
        imageUrl: String,
        force: Boolean,
        intent: MangaPageAcquisitionIntent,
        options: ProgressiveImageDecodeOptions,
        onFetch: () -> Unit,
        onProgressiveState: (StateFlow<ProgressiveImageState>?) -> Unit,
        fetch: suspend () -> Response,
    ): File {
        onProgressiveState(null)
        return store.getOrPutImage(imageUrl, force, onFetch, fetch)
    }
}
