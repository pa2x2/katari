package app.katari.entry.interactions.manga.page.acquisition

import app.katari.core.image.progressive.ProgressiveImageDecodeOptions
import app.katari.core.image.progressive.ProgressiveImageState
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Response
import java.io.File

internal interface MangaPageAcquirer {
    /** Whether visible page work must supersede cache-warming requests. */
    val prioritizesVisiblePages: Boolean

    suspend fun acquire(
        imageUrl: String,
        force: Boolean,
        intent: MangaPageAcquisitionIntent,
        options: ProgressiveImageDecodeOptions = ProgressiveImageDecodeOptions(),
        onFetch: () -> Unit = {},
        onProgressiveState: (StateFlow<ProgressiveImageState>?) -> Unit = {},
        fetch: suspend () -> Response,
    ): File
}
