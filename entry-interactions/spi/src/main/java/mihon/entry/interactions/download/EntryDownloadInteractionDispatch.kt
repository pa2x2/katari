package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class EntryDownloadInteractionDispatch(
    private val processors: Map<EntryType, EntryDownloadProcessor>,
    private val optionsProcessors: Map<EntryType, EntryDownloadOptionsProcessor>,
    private val bulkCandidateProcessors: Map<EntryType, EntryBulkDownloadCandidateProcessor>,
) : EntryDownloadInteraction {
    private val paused = MutableStateFlow(false)

    override val changes: Flow<Unit> = processors.values.map { it.changes }.merged()
    override val isInitializing: Flow<Boolean> = processors.values.map { it.isInitializing }.combinedAny()
    override val isRunning: Flow<Boolean> = processors.values.map { it.isRunning }.combinedAny()
    override val isPaused: Flow<Boolean> = paused.asStateFlow()
    override val queueState: Flow<List<EntryDownloadQueueGroup>> = processors.values
        .map { it.queueState }
        .combinedFlatten()

    override fun updates(): Flow<EntryDownloadStatus> {
        return processors.values.map { it.updates() }.merged()
    }

    override fun queueStatusUpdates(): Flow<EntryDownloadQueueItem> {
        return processors.values.map { it.queueStatusUpdates() }.merged()
    }

    override fun queueProgressUpdates(): Flow<EntryDownloadQueueItem> {
        return processors.values.map { it.queueProgressUpdates() }.merged()
    }

    override fun events(): Flow<EntryDownloadEvent> {
        return processors.values.map { it.events }.merged()
    }

    override suspend fun runDownloadsUntilIdle() = coroutineScope {
        processors.values
            .map { processor -> async { processor.runDownloadsUntilIdle() } }
            .awaitAll()
        Unit
    }

    override fun startDownloads() {
        paused.value = false
        processors.values.forEach { it.startDownloads() }
    }

    override fun pauseDownloads() {
        processors.values.forEach { it.pauseDownloads() }
        paused.value = true
    }

    override fun clearQueue() {
        processors.values.forEach { it.clearQueue() }
        paused.value = false
    }

    override fun invalidateCaches() {
        processors.values.forEach { it.invalidateCache() }
    }

    override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        processors.values.forEach { it.renameSource(oldSource, newSource) }
    }

    override suspend fun renameEntry(entry: Entry, newTitle: String) {
        val processor = processors[entry.type] ?: return
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.renameEntry(entry, newTitle)
    }

    override fun reorderQueue(items: List<EntryDownloadQueueItem>) {
        items.groupBy { it.entryType }
            .forEach { (type, typedItems) ->
                processors.requireProcessor("download", type).reorderQueue(typedItems)
            }
    }

    override fun reorderSeries(entryType: EntryType, entryId: Long, moveToTop: Boolean) {
        processors.requireProcessor("download", entryType).reorderSeries(entryId, moveToTop)
    }

    override fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>) {
        items.groupBy { it.entryType }
            .forEach { (type, typedItems) ->
                processors.requireProcessor("download", type).cancelQueuedDownloads(typedItems)
            }
    }

    override suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.queue(entry, chapters, autoStart)
        if (autoStart) paused.value = false
    }

    override suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean) {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.download(entry, chapters, startNow)
        paused.value = false
    }

    override suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ) {
        val processor = optionsProcessors.requireProcessor("download options", entry.type)
        processor.requireMatchingEntryType("download options", entry, optionsProcessors.keys)
        processor.downloadWithOptions(entry, chapters, selection, startNow)
        paused.value = false
    }

    override suspend fun resolveDownloadOptions(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptions? {
        val processor = optionsProcessors[entry.type] ?: return null
        processor.requireMatchingEntryType("download options", entry, optionsProcessors.keys)
        return processor.resolveDownloadOptions(context, entry, chapter)
    }

    override suspend fun resolveBulkDownloadCandidatePool(
        entry: Entry,
        candidates: List<EntryChapter>?,
    ): List<EntryChapter> {
        val processor = bulkCandidateProcessors.requireProcessor("bulk download candidates", entry.type)
        processor.requireMatchingEntryType("bulk download candidates", entry, bulkCandidateProcessors.keys)
        return processor.resolveBulkDownloadCandidatePool(entry, candidates)
    }

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        val processor = processors[entry.type] ?: return
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.delete(entry, chapters)
    }

    override suspend fun cleanup(entry: Entry, chapters: List<EntryChapter>) {
        val processor = processors[entry.type] ?: return
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.cleanup(entry, chapters)
    }

    override suspend fun deleteEntryDownloads(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.deleteEntryDownloads(entry)
    }

    override fun hasDownloads(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.hasDownloads(entry)
    }

    override fun getDownloadCount(entry: Entry): Int {
        val processor = processors[entry.type] ?: return 0
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.getDownloadCount(entry)
    }

    override fun getTotalDownloadCount(): Int {
        return processors.values.sumOf { it.getTotalDownloadCount() }
    }

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.isDownloaded(entry, chapter, skipCache)
    }

    override fun getStatus(
        entryType: EntryType,
        chapterId: Long,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus {
        val processor = processors[entryType]
            ?: return EntryDownloadStatus(entryType, chapterId, EntryDownloadState.NOT_DOWNLOADED)
        return processor.getStatus(
            chapterId = chapterId,
            chapterName = chapterName,
            chapterScanlator = chapterScanlator,
            chapterUrl = chapterUrl,
            entryTitle = entryTitle,
            sourceId = sourceId,
        )
    }

    override fun cancelQueuedDownload(entryType: EntryType, chapterId: Long): EntryDownloadStatus? {
        return processors[entryType]?.cancelQueuedDownload(chapterId)
    }
}
