package mihon.entry.interactions.translate

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.runtime.applicableProviderTypes
import mihon.entry.interactions.translate.queue.EntryTranslateFailureCodec
import mihon.entry.interactions.translate.queue.EntryTranslateSetupCodec
import mihon.entry.interactions.translate.work.EntryTranslateConditions
import mihon.entry.interactions.translate.work.EntryTranslateQueueRunner
import mihon.entry.interactions.translate.work.EntryTranslateWorkController
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
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
    private val languages: EntryTranslationLanguagesFeature,
    private val download: EntryDownloadInteraction,
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
    private val runner: EntryTranslateQueueRunner,
    private val work: EntryTranslateWorkController,
    conditions: EntryTranslateConditions,
    private val preferences: EntryTranslatePreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) : EntryTranslateFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryTranslateProcessor>(
        feature = ENTRY_TRANSLATE_FEATURE_ID,
        integration = ENTRY_TRANSLATE_INTEGRATION_ID,
        behaviorProjection = EntryTranslateBehavior.id,
    )

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override val waiting: Flow<EntryTranslateWaiting?> = conditions.waiting

    override val paused: Flow<Boolean> = conditions.paused

    override fun pause() {
        // The worker sees the preference change, stops the chapter it is on and ends.
        preferences.queuePaused.set(true)
    }

    override fun resume() {
        preferences.queuePaused.set(false)
        work.start()
    }

    override val queue: Flow<List<EntryTranslateQueueItem>> =
        combine(repository.subscribeAll(), runner.active) { items, active ->
            items.map { item -> EntryTranslateQueueItem(item.entryId, item.chapterId, item.status(active)) }
        }

    override suspend fun prepare(entry: Entry): EntryTranslatePreparation? {
        if (!isApplicable(entry.type)) return null
        return translate.prepare(entry, languages.observe(entry).first())
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
        val preparations = mutableMapOf<Long, EntryTranslatePreparation?>()
        failed.forEach { item ->
            val entry = entries.getEntryById(item.entryId)
            val chapter = chapters.getChapterById(item.chapterId)
            if (entry == null || chapter == null) {
                repository.delete(listOf(item.chapterId))
                return@forEach
            }
            // The frozen setup stopped working, so the chapter is translated with what the settings resolve to now.
            val setup = if (item.needsSetup()) {
                val preparation = preparations.getOrPut(entry.id) { prepare(entry) }
                (preparation as? EntryTranslatePreparation.Ready)?.setup ?: return@forEach
            } else {
                null
            }
            val downloaded = download.isDownloaded(entry, chapter)
            val state = if (downloaded) {
                EntryTranslationQueueItem.State.Queued
            } else {
                EntryTranslationQueueItem.State.WaitingForDownload
            }
            if (setup != null) {
                repository.enqueue(
                    entry.id,
                    listOf(chapter.id),
                    state,
                    EntryTranslateSetupCodec.encode(setup),
                    first = false,
                    queuedAt = clock(),
                )
            } else {
                repository.setState(item.chapterId, state)
            }
            // A chapter whose download failed or went away is downloaded again before it is translated.
            if (!downloaded) download.download(entry, listOf(chapter))
            ready = ready || downloaded
        }
        if (ready) work.start()
    }

    override suspend fun translateWithCurrentSettings(entry: Entry, chapters: List<EntryChapter>) {
        if (!isApplicable(entry.type) || chapters.isEmpty()) return
        when (val preparation = prepare(entry)) {
            is EntryTranslatePreparation.Ready -> translate(entry, chapters, preparation.setup, startNow = false)
            is EntryTranslatePreparation.Blocked -> repository.enqueue(
                entry.id,
                chapters.map { it.id },
                EntryTranslationQueueItem.State.Failed,
                setup = null,
                first = false,
                queuedAt = clock(),
                failure = EntryTranslateFailureCodec.encode(EntryTranslateFailure.SetupRequired),
            )
            null -> Unit
        }
    }

    override suspend fun untranslatedDownloads(entry: Entry): List<EntryChapter> {
        if (!isApplicable(entry.type)) return emptyList()
        val downloaded = chapters.getChaptersByEntryIdAwait(entry.id).filter { download.isDownloaded(entry, it) }
        val translated = translate.translatedChapters(entry, downloaded)
        val queued = repository.getAll().mapTo(HashSet()) { it.chapterId }
        return downloaded.filter { it.id !in translated && it.id !in queued }
    }

    override suspend fun cancel(chapterIds: List<Long>) {
        repository.delete(chapterIds)
        runner.stop(chapterIds)
    }

    override suspend fun cancelAll() {
        cancel(repository.getAll().map { it.chapterId })
    }

    override suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>) {
        if (!isApplicable(entry.type)) return
        translate.deleteTranslation(entry, chapters)
    }

    private fun EntryTranslationQueueItem.needsSetup(): Boolean =
        setup == null || EntryTranslateFailureCodec.decode(failure) == EntryTranslateFailure.SetupRequired

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
