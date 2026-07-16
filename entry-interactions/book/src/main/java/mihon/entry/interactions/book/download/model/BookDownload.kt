package mihon.entry.interactions.book.download.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class BookDownload(
    val entry: Entry,
    val chapter: EntryChapter,
) {
    private val _statusFlow = MutableStateFlow(State.NOT_DOWNLOADED)
    val statusFlow = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(value) {
            _statusFlow.value = value
        }

    private val _progressFlow = MutableStateFlow(0)
    val progressFlow = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value.coerceIn(0, 100)
        }

    private val _failureFlow = MutableStateFlow<BookDownloadFailure?>(null)
    val failureFlow = _failureFlow.asStateFlow()
    var failure: BookDownloadFailure?
        get() = _failureFlow.value
        set(value) {
            _failureFlow.value = value
        }

    enum class State {
        NOT_DOWNLOADED,
        QUEUE,
        RESOLVING,
        DOWNLOADING,
        DOWNLOADED,
        ERROR,
    }
}

internal data class BookDownloadFailure(
    val reason: Reason,
    val message: String? = null,
) {
    enum class Reason {
        SOURCE_NOT_FOUND,
        CONTENT_UNAVAILABLE,
        UNSUPPORTED_FORMAT,
        AMBIGUOUS_RESOURCE,
        STORAGE,
        INTEGRITY,
        NETWORK,
        UNKNOWN,
    }
}
