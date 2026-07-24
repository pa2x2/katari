package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal interface EntryDownloadRuntimeCoordinator : EntryDownloadRuntimeFeature {
    fun events(): Flow<EntryDownloadEvent>

    suspend fun runDownloadsUntilIdle()
}

internal class DefaultEntryDownloadRuntimeFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryDownloadInteraction,
) : EntryDownloadRuntimeCoordinator {
    private val applicableTypes = EntryDownloadRuntimeBehavior.entries
        .map { behavior ->
            evaluation.applicableProviderTypes<EntryDownloadProcessor>(
                feature = ENTRY_DOWNLOAD_RUNTIME_FEATURE_ID,
                integration = ENTRY_DOWNLOAD_RUNTIME_INTEGRATION_ID,
                behaviorProjection = behavior.id,
            )
        }
        .also { selectedTypes ->
            check(selectedTypes.distinct().size <= 1) {
                "Download runtime behaviors selected different provider sets: $selectedTypes"
            }
        }
        .firstOrNull()
        .orEmpty()

    override val changes: Flow<Unit> = interaction.changes

    override val state: Flow<EntryDownloadRuntimeState> = combine(
        interaction.queueState,
        interaction.isInitializing,
        interaction.isRunning,
        interaction.isPaused,
    ) { queue, isInitializing, isRunning, isPaused ->
        EntryDownloadRuntimeState(
            queue = queue.filter { it.entryType in applicableTypes },
            isInitializing = isInitializing && applicableTypes.isNotEmpty(),
            isRunning = isRunning && applicableTypes.isNotEmpty(),
            isPaused = isPaused && applicableTypes.isNotEmpty(),
        )
    }

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun statusUpdates(): Flow<EntryDownloadStatus> = interaction.updates()
        .filter { isApplicable(it.entryType) }

    override fun queueStatusUpdates(): Flow<EntryDownloadQueueItem> = interaction.queueStatusUpdates()
        .filter { isApplicable(it.entryType) }

    override fun queueProgressUpdates(): Flow<EntryDownloadQueueItem> = interaction.queueProgressUpdates()
        .filter { isApplicable(it.entryType) }

    override fun start() {
        if (applicableTypes.isNotEmpty()) interaction.startDownloads()
    }

    override fun pause() {
        if (applicableTypes.isNotEmpty()) interaction.pauseDownloads()
    }

    override fun clearQueue() {
        if (applicableTypes.isNotEmpty()) interaction.clearQueue()
    }

    override fun reorderQueue(items: List<EntryDownloadQueueItem>) {
        interaction.reorderQueue(items.filter { isApplicable(it.entryType) })
    }

    override fun reorderEntry(type: EntryType, entryId: Long, moveToTop: Boolean) {
        if (isApplicable(type)) interaction.reorderSeries(type, entryId, moveToTop)
    }

    override fun cancelQueued(items: List<EntryDownloadQueueItem>) {
        interaction.cancelQueuedDownloads(items.filter { isApplicable(it.entryType) })
    }

    override fun cancelQueued(type: EntryType, childId: Long): EntryDownloadStatus? {
        if (!isApplicable(type)) return null
        return interaction.cancelQueuedDownload(type, childId)
    }

    override fun downloadCount(entry: Entry): Int {
        if (!isApplicable(entry.type)) return 0
        return interaction.getDownloadCount(entry)
    }

    override fun totalDownloadCount(): Int = interaction.getTotalDownloadCount()

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean {
        if (!isApplicable(entry.type)) return false
        return interaction.isDownloaded(entry, chapter, skipCache)
    }

    override fun status(
        type: EntryType,
        childId: Long,
        childName: String,
        childScanlator: String?,
        childUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus? {
        if (!isApplicable(type)) return null
        return interaction.getStatus(
            entryType = type,
            chapterId = childId,
            chapterName = childName,
            chapterScanlator = childScanlator,
            chapterUrl = childUrl,
            entryTitle = entryTitle,
            sourceId = sourceId,
        )
    }

    override fun events(): Flow<EntryDownloadEvent> = interaction.events()
        .filter { event -> event.entryTypeOrNull()?.let(::isApplicable) != false }

    override suspend fun runDownloadsUntilIdle() {
        if (applicableTypes.isNotEmpty()) interaction.runDownloadsUntilIdle()
    }
}

private fun EntryDownloadEvent.entryTypeOrNull(): EntryType? = when (this) {
    is EntryDownloadEvent.Error -> entryType
    is EntryDownloadEvent.Warning -> null
}
