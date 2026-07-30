package mihon.entry.interactions.book

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.entry.interactions.EntryChildWebViewResolution
import mihon.entry.interactions.EntryWebViewFeature

internal class BookChildWebViewResolver(
    private val scope: CoroutineScope,
    private val feature: EntryWebViewFeature,
    private val currentChapterId: () -> Long?,
    private val onResolution: (EntryChildWebViewResolution.Available?) -> Unit,
    private val onFailure: (Throwable) -> Unit,
) : AutoCloseable {
    private var resolutionJob: Job? = null

    fun resolve(session: OpenedBookReaderSession) {
        resolutionJob?.cancel()
        onResolution(null)
        resolutionJob = scope.launch {
            val resolution = withContext(Dispatchers.IO) {
                feature.resolveChild(session.owner, session.chapter)
            }
            if (currentChapterId() != session.chapter.id) return@launch
            when (resolution) {
                is EntryChildWebViewResolution.Available -> onResolution(resolution)
                is EntryChildWebViewResolution.Failed -> onFailure(resolution.cause)
                else -> Unit
            }
        }
    }

    override fun close() {
        resolutionJob?.cancel()
        resolutionJob = null
    }
}
