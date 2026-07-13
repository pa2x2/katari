package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

fun createEntryInteractions(
    plugins: List<EntryInteractionPlugin>,
): EntryInteractions {
    val registry = DefaultEntryInteractionRegistry()
    plugins.forEach { it.register(registry) }
    return registry.createEntryInteractions()
}

private class DefaultEntryInteractionRegistry : EntryInteractionRegistry {
    private val openProcessors = mutableMapOf<EntryType, EntryOpenProcessor>()
    private val continueProcessors = mutableMapOf<EntryType, EntryContinueProcessor>()
    private val downloadProcessors = mutableMapOf<EntryType, EntryDownloadProcessor>()
    private val capabilityProcessors = mutableMapOf<EntryType, EntryCapabilityProcessor>()
    private val consumptionProcessors = mutableMapOf<EntryType, EntryConsumptionProcessor>()
    private val updateEligibilityProcessors = mutableMapOf<EntryType, EntryUpdateEligibilityProcessor>()
    private val progressProcessors = mutableMapOf<EntryType, EntryProgressProcessor>()
    private val playbackProcessors = mutableMapOf<EntryType, EntryPlaybackProcessor>()
    private val childListProcessors = mutableMapOf<EntryType, EntryChildListProcessor>()
    private val childGroupFilterProcessors = mutableMapOf<EntryType, EntryChildGroupFilterProcessor>()
    private val libraryFilterProcessors = mutableMapOf<EntryType, EntryLibraryFilterProcessor>()
    private val previewProcessors = mutableMapOf<EntryType, EntryPreviewInteraction>()
    private val immersiveFeedProcessors = mutableMapOf<EntryType, EntryImmersiveFeedProcessor>()

    override fun registerOpenProcessor(processor: EntryOpenProcessor) {
        registerProcessor("open", processor.type, processor, openProcessors)
    }

    override fun registerContinueProcessor(processor: EntryContinueProcessor) {
        registerProcessor("continue", processor.type, processor, continueProcessors)
    }

    override fun registerDownloadProcessor(processor: EntryDownloadProcessor) {
        registerProcessor("download", processor.type, processor, downloadProcessors)
    }

    override fun registerCapabilityProcessor(processor: EntryCapabilityProcessor) {
        registerProcessor("capability", processor.type, processor, capabilityProcessors)
    }

    override fun registerConsumptionProcessor(processor: EntryConsumptionProcessor) {
        registerProcessor("consumption", processor.type, processor, consumptionProcessors)
    }

    override fun registerUpdateEligibilityProcessor(processor: EntryUpdateEligibilityProcessor) {
        registerProcessor("update eligibility", processor.type, processor, updateEligibilityProcessors)
    }

    override fun registerProgressProcessor(processor: EntryProgressProcessor) {
        registerProcessor("progress", processor.type, processor, progressProcessors)
    }

    override fun registerPlaybackProcessor(processor: EntryPlaybackProcessor) {
        registerProcessor("playback", processor.type, processor, playbackProcessors)
    }

    override fun registerChildListProcessor(processor: EntryChildListProcessor) {
        registerProcessor("child list", processor.type, processor, childListProcessors)
    }

    override fun registerChildGroupFilterProcessor(processor: EntryChildGroupFilterProcessor) {
        registerProcessor("child group filter", processor.type, processor, childGroupFilterProcessors)
    }

    override fun registerLibraryFilterProcessor(processor: EntryLibraryFilterProcessor) {
        registerProcessor("library filter", processor.type, processor, libraryFilterProcessors)
    }

    override fun registerPreviewProcessor(processor: EntryPreviewProcessor) {
        registerProcessor("preview", processor.type, processor, previewProcessors)
    }

    override fun registerImmersiveFeedProcessor(processor: EntryImmersiveFeedProcessor) {
        registerProcessor("immersive feed", processor.type, processor, immersiveFeedProcessors)
    }

    fun createEntryInteractions(): EntryInteractions {
        return DefaultEntryInteractions(
            openProcessors = openProcessors.toMap(),
            continueProcessors = continueProcessors.toMap(),
            downloadProcessors = downloadProcessors.toMap(),
            capabilityProcessors = capabilityProcessors.toMap(),
            consumptionProcessors = consumptionProcessors.toMap(),
            updateEligibilityProcessors = updateEligibilityProcessors.toMap(),
            progressProcessors = progressProcessors.toMap(),
            playbackProcessors = playbackProcessors.toMap(),
            childListProcessors = childListProcessors.toMap(),
            childGroupFilterProcessors = childGroupFilterProcessors.toMap(),
            libraryFilterProcessors = libraryFilterProcessors.toMap(),
            previewProcessors = previewProcessors.toMap(),
            immersiveFeedProcessors = immersiveFeedProcessors.toMap(),
        )
    }

    private fun <T> registerProcessor(
        category: String,
        type: EntryType,
        processor: T,
        processors: MutableMap<EntryType, T>,
    ) {
        val previous = processors.putIfAbsent(type, processor)
        check(previous == null) {
            "Duplicate $category processor registered for EntryType $type. Registered types: " +
                processors.registeredTypes()
        }
    }
}

private class DefaultEntryInteractions(
    openProcessors: Map<EntryType, EntryOpenProcessor>,
    continueProcessors: Map<EntryType, EntryContinueProcessor>,
    downloadProcessors: Map<EntryType, EntryDownloadProcessor>,
    capabilityProcessors: Map<EntryType, EntryCapabilityProcessor>,
    consumptionProcessors: Map<EntryType, EntryConsumptionProcessor>,
    updateEligibilityProcessors: Map<EntryType, EntryUpdateEligibilityProcessor>,
    progressProcessors: Map<EntryType, EntryProgressProcessor>,
    playbackProcessors: Map<EntryType, EntryPlaybackProcessor>,
    childListProcessors: Map<EntryType, EntryChildListProcessor>,
    childGroupFilterProcessors: Map<EntryType, EntryChildGroupFilterProcessor>,
    libraryFilterProcessors: Map<EntryType, EntryLibraryFilterProcessor>,
    previewProcessors: Map<EntryType, EntryPreviewInteraction>,
    immersiveFeedProcessors: Map<EntryType, EntryImmersiveFeedProcessor>,
) : EntryInteractions {
    override val open: EntryOpenInteraction = RegistryEntryOpenInteraction(openProcessors)
    override val continueEntry: EntryContinueInteraction = RegistryEntryContinueInteraction(continueProcessors)
    override val download: EntryDownloadInteraction = RegistryEntryDownloadInteraction(downloadProcessors)
    override val capability: EntryCapabilityInteraction =
        RegistryEntryCapabilityInteraction(capabilityProcessors, downloadProcessors)
    override val consumption: EntryConsumptionInteraction = RegistryEntryConsumptionInteraction(consumptionProcessors)
    override val updateEligibility: EntryUpdateEligibilityInteraction =
        RegistryEntryUpdateEligibilityInteraction(updateEligibilityProcessors)
    override val progress: EntryProgressInteraction = RegistryEntryProgressInteraction(progressProcessors)
    override val playback: EntryPlaybackInteraction = RegistryEntryPlaybackInteraction(playbackProcessors)
    override val childList: EntryChildListInteraction = RegistryEntryChildListInteraction(childListProcessors)
    override val childGroupFilter: EntryChildGroupFilterInteraction =
        RegistryEntryChildGroupFilterInteraction(childGroupFilterProcessors)
    override val libraryFilter: EntryLibraryFilterInteraction =
        RegistryEntryLibraryFilterInteraction(libraryFilterProcessors)
    override val preview: EntryPreviewInteraction = RegistryEntryPreviewInteraction(previewProcessors)
    override val immersiveFeed: EntryImmersiveFeedInteraction =
        RegistryEntryImmersiveFeedInteraction(immersiveFeedProcessors)
}

private class RegistryEntryOpenInteraction(
    private val processors: Map<EntryType, EntryOpenProcessor>,
) : EntryOpenInteraction {
    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        val processor = processors.requireProcessor("open", entry.type)
        processor.requireMatchingEntryType("open", entry, processors.keys)
        processor.open(context, entry, chapter, options)
    }

    override fun pendingIntent(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) =
        processors.requireProcessor("open", entry.type).also {
            it.requireMatchingEntryType("open", entry, processors.keys)
        }.pendingIntent(context, entry, chapter, options)
}

private class RegistryEntryPreviewInteraction(
    private val processors: Map<EntryType, EntryPreviewInteraction>,
) : EntryPreviewInteraction {
    override fun isSupported(entry: Entry): Boolean {
        return processors[entry.type]?.isSupported(entry) ?: false
    }

    override fun requiresChapter(entry: Entry): Boolean {
        return processors[entry.type]?.requiresChapter(entry) ?: true
    }

    override fun config(entry: Entry): EntryPreviewConfig {
        return processors[entry.type]?.config(entry) ?: EntryPreviewConfig.Disabled
    }

    override fun configChanges(entry: Entry): Flow<EntryPreviewConfig> {
        return processors[entry.type]?.configChanges(entry) ?: flowOf(EntryPreviewConfig.Disabled)
    }

    override suspend fun loadPreview(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle {
        val processor = processors.requireProcessor("preview", entry.type)
        return processor.loadPreview(context, entry, chapter, source, pageCount)
    }

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) {
        val processor = processors.requireProcessor("preview", handle.entryType)
        processor.loadPage(handle, pageIndex)
    }

    override fun release(handle: EntryPreviewHandle) {
        processors[handle.entryType]?.release(handle)
    }
}

private class RegistryEntryImmersiveFeedInteraction(
    private val processors: Map<EntryType, EntryImmersiveFeedProcessor>,
) : EntryImmersiveFeedInteraction {
    override fun isSupported(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("immersive feed", entry, processors.keys)
        return processor.isSupported(entry)
    }

    override fun preloadRadius(entryType: EntryType): Int {
        return processors[entryType]?.preloadRadius(entryType) ?: 0
    }

    override suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        source: UnifiedSource,
    ): EntryImmersiveFeedHandle {
        val processor = processors.requireProcessor("immersive feed", entry.type)
        processor.requireMatchingEntryType("immersive feed", entry, processors.keys)
        return processor.load(context, entry, chapter, source)
    }

    override fun renderer(handle: EntryImmersiveFeedHandle): EntryImmersiveFeedRenderer {
        return processors.requireProcessor("immersive feed", handle.entryType).renderer(handle)
    }

    override suspend fun persistProgress(
        handle: EntryImmersiveFeedHandle,
        progress: EntryImmersiveFeedProgress,
    ) {
        processors.requireProcessor("immersive feed", handle.entryType)
            .persistProgress(handle, progress)
    }

    override fun release(handle: EntryImmersiveFeedHandle) {
        processors[handle.entryType]?.release(handle)
    }
}

private class RegistryEntryContinueInteraction(
    private val processors: Map<EntryType, EntryContinueProcessor>,
) : EntryContinueInteraction {
    override suspend fun continueEntry(context: Context, entry: Entry): EntryChapter? {
        val processor = processors.requireProcessor("continue", entry.type)
        processor.requireMatchingEntryType("continue", entry, processors.keys)
        val chapter = processor.findNext(entry)
        if (chapter != null) {
            processor.open(context, entry, chapter)
        }
        return chapter
    }

    override suspend fun findNext(entry: Entry): EntryChapter? {
        val processor = processors.requireProcessor("continue", entry.type)
        processor.requireMatchingEntryType("continue", entry, processors.keys)
        return processor.findNext(entry)
    }
}

private class RegistryEntryDownloadInteraction(
    private val processors: Map<EntryType, EntryDownloadProcessor>,
) : EntryDownloadInteraction {
    override val changes: Flow<Unit> = processors.values.map { it.changes }.merged()
    override val isInitializing: Flow<Boolean> = processors.values.map { it.isInitializing }.combinedAny()
    override val isRunning: Flow<Boolean> = processors.values.map { it.isRunning }.combinedAny()
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

    override fun startDownloads() {
        processors.values.forEach { it.startDownloads() }
    }

    override fun pauseDownloads() {
        processors.values.forEach { it.pauseDownloads() }
    }

    override fun clearQueue() {
        processors.values.forEach { it.clearQueue() }
    }

    override fun invalidateCaches() {
        processors.values.forEach { it.invalidateCache() }
    }

    override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        processors.values.forEach { it.renameSource(oldSource, newSource) }
    }

    override suspend fun renameEntry(entry: Entry, newTitle: String) {
        val processor = processors.requireProcessor("download", entry.type)
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
    }

    override suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean) {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.download(entry, chapters, startNow)
    }

    override suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ) {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.downloadWithOptions(entry, chapters, selection, startNow)
    }

    override fun supportsDownloadOptions(entry: Entry): Boolean {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.supportsDownloadOptions(entry)
    }

    override suspend fun resolveDownloadOptions(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptions? {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.resolveDownloadOptions(context, entry, chapter)
    }

    override fun supportsBulkDownload(entry: Entry): Boolean {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.supportsBulkDownload(entry)
    }

    override suspend fun resolveBulkDownloadCandidates(
        entry: Entry,
        action: EntryBulkDownloadAction,
        candidates: List<EntryChapter>?,
        memberEntryIds: List<Long>,
    ): EntryBulkDownloadCandidateResult {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.resolveBulkDownloadCandidates(entry, action, candidates, memberEntryIds)
    }

    override suspend fun filterAutoDownloadCandidates(
        entry: Entry,
        chapters: List<EntryChapter>,
    ): List<EntryChapter> {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.filterAutoDownloadCandidates(entry, chapters)
    }

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.delete(entry, chapters)
    }

    override suspend fun deleteEntryDownloads(entry: Entry) {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        processor.deleteEntryDownloads(entry)
    }

    override fun hasDownloads(entry: Entry): Boolean {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.hasDownloads(entry)
    }

    override fun getDownloadCount(entry: Entry): Int {
        val processor = processors.requireProcessor("download", entry.type)
        processor.requireMatchingEntryType("download", entry, processors.keys)
        return processor.getDownloadCount(entry)
    }

    override fun getTotalDownloadCount(): Int {
        return processors.values.sumOf { it.getTotalDownloadCount() }
    }

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean {
        val processor = processors.requireProcessor("download", entry.type)
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
        return processors.requireProcessor("download", entryType).getStatus(
            chapterId = chapterId,
            chapterName = chapterName,
            chapterScanlator = chapterScanlator,
            chapterUrl = chapterUrl,
            entryTitle = entryTitle,
            sourceId = sourceId,
        )
    }

    override fun cancelQueuedDownload(entryType: EntryType, chapterId: Long): EntryDownloadStatus? {
        return processors.requireProcessor("download", entryType).cancelQueuedDownload(chapterId)
    }
}

private class RegistryEntryCapabilityInteraction(
    private val processors: Map<EntryType, EntryCapabilityProcessor>,
    private val downloadProcessors: Map<EntryType, EntryDownloadProcessor>,
) : EntryCapabilityInteraction {
    override fun supportsMigration(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("capability", entry, processors.keys)
        return processor.supportsMigration(entry)
    }

    override fun canMigrate(entries: List<Entry>): Boolean {
        return entries.isNotEmpty() && entries.all(::supportsMigration)
    }

    override fun migrationEntries(entries: List<Entry>): List<Entry> {
        return entries.filter(::supportsMigration)
    }

    override fun supportsMerge(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("capability", entry, processors.keys)
        return processor.supportsMerge(entry)
    }

    override fun canMergeSelection(selection: List<EntryMergeCapabilityItem>): Boolean {
        if (selection.size < 2) return false
        if (selection.map { it.entry.type }.distinct().size != 1) return false
        if (selection.count { it.isMerged } > 1) return false

        return selection.all { supportsMerge(it.entry) }
    }

    override fun supportsBulkDownload(entry: Entry): Boolean {
        val processor = downloadProcessors[entry.type] ?: return false
        processor.requireMatchingEntryType("download", entry, downloadProcessors.keys)
        return processor.supportsBulkDownload(entry)
    }
}

private class RegistryEntryConsumptionInteraction(
    private val processors: Map<EntryType, EntryConsumptionProcessor>,
) : EntryConsumptionInteraction {
    override fun canSetConsumed(entryType: EntryType, status: EntryConsumptionStatus, consumed: Boolean): Boolean {
        val processor = processors.requireProcessor("consumption", entryType)
        return processor.canSetConsumed(status, consumed)
    }

    override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
        val processor = processors.requireProcessor("consumption", entry.type)
        processor.requireMatchingEntryType("consumption", entry, processors.keys)
        processor.setConsumed(entry, chapters, consumed)
    }

    override fun supportsBookmark(entryType: EntryType): Boolean {
        val processor = processors.requireProcessor("consumption", entryType)
        return processor.supportsBookmark
    }

    override fun canSetBookmarked(
        entryType: EntryType,
        status: EntryConsumptionStatus,
        bookmarked: Boolean,
    ): Boolean {
        val processor = processors.requireProcessor("consumption", entryType)
        return processor.canSetBookmarked(status, bookmarked)
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        val processor = processors.requireProcessor("consumption", entry.type)
        processor.requireMatchingEntryType("consumption", entry, processors.keys)
        if (processor.supportsBookmark) {
            processor.setBookmarked(entry, chapters, bookmarked)
        }
    }
}

private class RegistryEntryUpdateEligibilityInteraction(
    private val processors: Map<EntryType, EntryUpdateEligibilityProcessor>,
) : EntryUpdateEligibilityInteraction {
    override fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility {
        val processor = processors.requireProcessor("update eligibility", request.entry.type)
        processor.requireMatchingEntryType("update eligibility", request.entry, processors.keys)
        return processor.evaluate(request)
    }
}

private class RegistryEntryProgressInteraction(
    private val processors: Map<EntryType, EntryProgressProcessor>,
) : EntryProgressInteraction {
    override suspend fun snapshot(entry: Entry): EntryProgressSnapshot {
        val processor = processors[entry.type] ?: return EntryProgressSnapshot()
        processor.requireMatchingEntryType("progress", entry, processors.keys)
        return processor.snapshot(entry)
    }

    override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
        val processor = processors[entry.type] ?: return
        processor.requireMatchingEntryType("progress", entry, processors.keys)
        processor.restore(entry, snapshot)
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ) {
        if (sourceEntry.type != targetEntry.type) return
        val processor = processors[sourceEntry.type] ?: return
        processor.requireMatchingEntryType("progress", sourceEntry, processors.keys)
        processor.requireMatchingEntryType("progress", targetEntry, processors.keys)
        processor.copy(sourceEntry, targetEntry, resourceMappings)
    }
}

private class RegistryEntryPlaybackInteraction(
    private val processors: Map<EntryType, EntryPlaybackProcessor>,
) : EntryPlaybackInteraction {
    override suspend fun snapshot(entry: Entry): EntryPlaybackSnapshot {
        val processor = processors[entry.type] ?: return EntryPlaybackSnapshot()
        processor.requireMatchingEntryType("playback", entry, processors.keys)
        return processor.snapshot(entry)
    }

    override suspend fun restore(entry: Entry, snapshot: EntryPlaybackSnapshot) {
        val processor = processors[entry.type] ?: return
        processor.requireMatchingEntryType("playback", entry, processors.keys)
        processor.restore(entry, snapshot)
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        chapterMappings: List<EntryPlaybackChapterMapping>,
    ) {
        if (sourceEntry.type != targetEntry.type) return
        val processor = processors[sourceEntry.type] ?: return
        processor.requireMatchingEntryType("playback", sourceEntry, processors.keys)
        processor.requireMatchingEntryType("playback", targetEntry, processors.keys)
        processor.copy(sourceEntry, targetEntry, chapterMappings)
    }
}

private class RegistryEntryChildListInteraction(
    private val processors: Map<EntryType, EntryChildListProcessor>,
) : EntryChildListInteraction {
    override fun sortedForReading(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        val processor = processors.requireProcessor("child list", entry.type)
        processor.requireMatchingEntryType("child list", entry, processors.keys)
        return processor.sortedForReading(entry, chapters, memberIds)
    }

    override fun sortedForDisplay(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        val processor = processors.requireProcessor("child list", entry.type)
        processor.requireMatchingEntryType("child list", entry, processors.keys)
        return processor.sortedForDisplay(entry, chapters, memberIds)
    }

    override fun buildDisplayList(request: EntryChildListRequest): List<EntryChildListRow> {
        val processor = processors.requireProcessor("child list", request.entry.type)
        processor.requireMatchingEntryType("child list", request.entry, processors.keys)
        return processor.buildDisplayList(request)
    }

    override fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>> {
        val processor = processors.requireProcessor("child list", request.entry.type)
        processor.requireMatchingEntryType("child list", request.entry, processors.keys)
        return processor.progressLabels(request)
    }
}

private class RegistryEntryChildGroupFilterInteraction(
    private val processors: Map<EntryType, EntryChildGroupFilterProcessor>,
) : EntryChildGroupFilterInteraction {
    override fun supports(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("child group filter", entry, processors.keys)
        return processor.supports(entry)
    }

    override fun shouldApplyFilter(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("child group filter", entry, processors.keys)
        return processor.shouldApplyFilter(entry)
    }

    override fun availableGroupsChanged(entryId: Long): Flow<Unit> {
        return processors.values.map { it.availableGroupsChanged(entryId) }.merged()
    }

    override suspend fun availableGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        val processor = processors[entry.type] ?: return emptySet()
        processor.requireMatchingEntryType("child group filter", entry, processors.keys)
        if (!processor.supports(entry)) return emptySet()
        return processor.availableGroups(entry, memberIds)
    }

    override fun excludedGroupsChanged(entryId: Long): Flow<Unit> {
        return processors.values.map { it.excludedGroupsChanged(entryId) }.merged()
    }

    override suspend fun excludedGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        val processor = processors[entry.type] ?: return emptySet()
        processor.requireMatchingEntryType("child group filter", entry, processors.keys)
        if (!processor.supports(entry)) return emptySet()
        return processor.excludedGroups(entry, memberIds)
    }

    override suspend fun setExcludedGroups(entry: Entry, memberIds: Collection<Long>, excluded: Set<String>) {
        val processor = processors[entry.type] ?: return
        processor.requireMatchingEntryType("child group filter", entry, processors.keys)
        if (!processor.supports(entry)) return
        processor.setExcludedGroups(entry, memberIds, excluded)
    }
}

private class RegistryEntryLibraryFilterInteraction(
    private val processors: Map<EntryType, EntryLibraryFilterProcessor>,
) : EntryLibraryFilterInteraction {
    override fun supportsOutsideReleasePeriodFilter(entry: Entry): Boolean {
        val processor = processors[entry.type] ?: return false
        processor.requireMatchingEntryType("library filter", entry, processors.keys)
        return processor.supportsOutsideReleasePeriodFilter(entry)
    }
}

private fun <T> Map<EntryType, T>.requireProcessor(category: String, type: EntryType): T {
    return this[type] ?: error(
        "No $category processor registered for EntryType $type. Registered types: ${registeredTypes()}",
    )
}

private fun EntryOpenProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryContinueProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryDownloadProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryCapabilityProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryConsumptionProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryUpdateEligibilityProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryPlaybackProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryProgressProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryImmersiveFeedProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryChildListProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryChildGroupFilterProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun EntryLibraryFilterProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

private fun processorMismatchMessage(
    category: String,
    requestedType: EntryType,
    processorType: EntryType,
    registeredTypes: Set<EntryType>,
): String {
    return "Mismatched $category processor for EntryType $requestedType: processor type was $processorType. " +
        "Registered types: ${registeredTypes.registeredTypes()}"
}

private fun Map<EntryType, *>.registeredTypes(): String {
    return keys.registeredTypes()
}

private fun Set<EntryType>.registeredTypes(): String {
    return sortedBy { it.name }.joinToString().ifEmpty { "none" }
}

private fun <T> List<Flow<T>>.merged(): Flow<T> {
    return when (size) {
        0 -> emptyFlow()
        1 -> first()
        else -> channelFlow {
            forEach { flow ->
                launch {
                    flow.collect { send(it) }
                }
            }
        }
    }
}

private fun List<Flow<Boolean>>.combinedAny(): Flow<Boolean> {
    return when (size) {
        0 -> flowOf(false)
        1 -> first()
        else -> combine(this) { values -> values.any { it } }
    }
}

private fun List<Flow<List<EntryDownloadQueueGroup>>>.combinedFlatten(): Flow<List<EntryDownloadQueueGroup>> {
    return when (size) {
        0 -> flowOf(emptyList())
        1 -> first()
        else -> combine(this) { values -> values.flatMap { it } }
    }
}
