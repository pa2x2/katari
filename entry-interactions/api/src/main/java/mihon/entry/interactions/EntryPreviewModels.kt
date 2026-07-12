package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.StateFlow

data class EntryPreviewConfig(
    val enabled: Boolean,
    val pageCount: Int,
    val size: EntryPreviewSize,
) {
    companion object {
        val Disabled = EntryPreviewConfig(
            enabled = false,
            pageCount = 0,
            size = EntryPreviewSize.MEDIUM,
        )
    }
}

enum class EntryPreviewSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
}

data class EntryPreviewHandle(
    val entryType: EntryType,
    val chapterId: Long?,
    val pages: List<EntryPreviewPage>,
    val delegate: Any? = null,
)

data class EntryPreviewPage(
    val index: Int,
    val status: StateFlow<EntryPreviewPageStatus>,
    val progress: StateFlow<Int>,
    val imageModel: Any,
    val canOpen: Boolean = true,
)

sealed interface EntryPreviewPageStatus {
    data object Queued : EntryPreviewPageStatus
    data object LoadingPage : EntryPreviewPageStatus
    data object DownloadingImage : EntryPreviewPageStatus
    data object Ready : EntryPreviewPageStatus
    data class Error(val error: Throwable) : EntryPreviewPageStatus
}
