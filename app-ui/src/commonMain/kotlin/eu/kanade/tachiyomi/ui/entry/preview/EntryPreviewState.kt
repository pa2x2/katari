package eu.kanade.tachiyomi.ui.entry.preview

import androidx.compose.runtime.Immutable
import mihon.entry.interactions.media.EntryPreviewPage

@Immutable
data class EntryPreviewState(
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val chapterId: Long? = null,
    val pages: List<PreviewPage> = emptyList(),
    val pageCount: Int = 5,
    val hasLoaded: Boolean = false,
) {
    val hasLoadedContent: Boolean
        get() = (hasLoaded || chapterId != null) && !isLoading && error == null
}

@Immutable
data class PreviewPage(
    val page: EntryPreviewPage,
)
