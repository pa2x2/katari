package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.flow.StateFlow
import mihon.core.common.image.progressive.ProgressiveImageState
import java.io.InputStream

internal open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    var stream: (() -> InputStream)? = null,
) : Page(index, url, imageUrl, null) {

    open lateinit var chapter: ReaderChapter

    var progressiveImageState: StateFlow<ProgressiveImageState>? = null
}
