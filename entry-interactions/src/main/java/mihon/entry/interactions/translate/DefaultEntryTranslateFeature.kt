package mihon.entry.interactions.translate

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.runtime.applicableProviderTypes
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryTranslationQueueItem
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultEntryTranslateFeature(
    evaluation: FeatureGraphEvaluation,
    private val repository: EntryTranslationQueueRepository,
    private val translate: EntryTranslateInteraction,
    private val download: EntryDownloadInteraction,
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
    private val runner: EntryTranslateQueueRunner,
    private val work: EntryTranslateWorkController,
    private val clock: () -> Long = System::currentTimeMillis,
) : EntryTranslateFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryTranslateProcessor>(
        feature = ENTRY_TRANSLATE_FEATURE_ID,
        integration = ENTRY_TRANSLATE_INTEGRATION_ID,
        behaviorProjection = EntryTranslateBehavior.id,
    )

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override val queue: Flow<List<EntryTranslateQueueItem>> =
        combine(repository.subscribeAll(), runner.active) { items, active ->
            items.map { item -> EntryTranslateQueueItem(item.entryId, item.chapterId, item.status(active)) }
        }

    override fun observeStatuses(entry: Entry): Flow<Map<Long, EntryTranslateStatus>> {
        if (!isApplicable(entry.type)) return flowOf(emptyMap())
        // Only downloaded chapters can have a stored translation, and it goes when they do.
        val translated = combine(
            chapters.getChaptersByEntryId(entry.id),
            translate.changes.onStart { emit(Unit) },
            download.changes.onStart { emit(Unit) },
        ) { entryChapters, _, _ -> entryChapters }
            .mapLatest { entryChapters ->
                translate.translatedChapters(entry, entryChapters.filter { download.isDownloaded(entry, it) })
            }
        val queued = combine(repository.subscribeAll(), runner.active) { items, active ->
            items.filter { it.entryId == entry.id }.associate { it.chapterId to it.status(active) }
        }
        return combine(translated, queued) { done, pending ->
            done.associateWith { EntryTranslateStatus.Translated } + pending
        }.distinctUntilChanged()
    }

    override suspend fun translate(
        entry: Entry,
        chapters: List<EntryChapter>,
        setup: EntryTranslateSetup,
        startNow: Boolean,
    ) {
        if (!isApplicable(entry.type) || chapters.isEmpty()) return
        val (downloaded, waiting) = chapters.partition { download.isDownloaded(entry, it) }
        val encoded = EntryTranslateSetupCodec.encode(setup)
        mapOf(
            EntryTranslationQueueItem.State.WaitingForDownload to waiting,
            EntryTranslationQueueItem.State.Queued to downloaded,
        )
            .filterValues { it.isNotEmpty() }
            .forEach { (state, group) ->
                repository.enqueue(entry.id, group.map { it.id }, state, encoded, first = startNow, queuedAt = clock())
            }
        if (downloaded.isNotEmpty()) work.start()
    }

    override suspend fun startNow(chapterIds: List<Long>) {
        repository.moveToFront(chapterIds)
        work.start()
    }

    override suspend fun retry(chapterIds: List<Long>) {
        val failed = repository.getAll()
            .filter { it.chapterId in chapterIds && it.state == EntryTranslationQueueItem.State.Failed }
        var ready = false
        failed.forEach { item ->
            val entry = entries.getEntryById(item.entryId)
            val chapter = chapters.getChapterById(item.chapterId)
            if (entry == null || chapter == null) {
                repository.delete(listOf(item.chapterId))
            } else if (download.isDownloaded(entry, chapter)) {
                repository.setState(item.chapterId, EntryTranslationQueueItem.State.Queued)
                ready = true
            } else {
                // A chapter whose download failed or went away is downloaded again before it is translated.
                repository.setState(item.chapterId, EntryTranslationQueueItem.State.WaitingForDownload)
                download.download(entry, listOf(chapter))
            }
        }
        if (ready) work.start()
    }

    override suspend fun cancel(chapterIds: List<Long>) {
        repository.delete(chapterIds)
        runner.stop(chapterIds)
    }

    override suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>) {
        if (!isApplicable(entry.type)) return
        translate.deleteTranslation(entry, chapters)
    }

    private fun EntryTranslationQueueItem.status(active: EntryTranslateQueueRunner.Active?) = when (state) {
        EntryTranslationQueueItem.State.WaitingForDownload -> EntryTranslateStatus.WaitingForDownload
        EntryTranslationQueueItem.State.Failed -> EntryTranslateStatus.Failed(
            EntryTranslateFailureCodec.decode(failure),
        )
        EntryTranslationQueueItem.State.Queued -> active?.takeIf { it.chapter.id == chapterId }
            ?.let { EntryTranslateStatus.Translating(it.progress) }
            ?: EntryTranslateStatus.Queued
    }
}
