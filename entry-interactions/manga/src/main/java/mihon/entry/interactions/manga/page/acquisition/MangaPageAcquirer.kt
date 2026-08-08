package mihon.entry.interactions.manga.page.acquisition

import kotlinx.coroutines.flow.StateFlow
import mihon.core.common.image.progressive.ProgressiveImageDecodeOptions
import mihon.core.common.image.progressive.ProgressiveImageState
import okhttp3.Response
import java.io.File

internal interface MangaPageAcquirer {
    suspend fun acquire(
        imageUrl: String,
        force: Boolean,
        options: ProgressiveImageDecodeOptions = ProgressiveImageDecodeOptions(),
        onFetch: () -> Unit = {},
        onProgressiveState: (StateFlow<ProgressiveImageState>?) -> Unit = {},
        fetch: suspend () -> Response,
    ): File
}
