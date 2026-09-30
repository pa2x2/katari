package eu.kanade.tachiyomi.ui.download.translation

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import eu.kanade.presentation.entry.translation.TranslatableChapter
import eu.kanade.tachiyomi.ui.entry.translation.ChapterTranslationModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslateQueueItem
import mihon.entry.interactions.translate.EntryTranslateWaiting
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The translation queue as the queue screen lists it, with pausing, cancelling and retrying its chapters. */
@OptIn(ExperimentalCoroutinesApi::class)
class TranslationQueueScreenModel(
    application: Application = Injekt.get(),
    private val feature: EntryTranslateFeature = Injekt.get(),
    private val entries: EntryRepository = Injekt.get(),
    private val chapters: EntryChapterRepository = Injekt.get(),
    private val downloads: EntryDownloadRuntimeFeature = Injekt.get(),
    private val downloadAction: EntryDownloadActionFeature = Injekt.get(),
) : ScreenModel {
    val snackbarHostState = SnackbarHostState()

    /** Settles what blocks chapters that failed for want of setup before they are queued again. */
    val translation = ChapterTranslationModel(
        scope = screenModelScope,
        context = application,
        snackbarHostState = snackbarHostState,
        download = { items ->
            screenModelScope.launch {
                items.groupBy { it.entry }.forEach { (entry, group) ->
                    downloadAction.download(entry, group.map { it.chapter }, startNow = false)
                }
            }
        },
        feature = feature,
        languages = Injekt.get(),
        recognitionHost = Injekt.get(),
        translationHost = Injekt.get(),
        modelStore = Injekt.get(),
    )

    /** The queue in processing order; null until first loaded. */
    val rows: StateFlow<List<TranslationQueueRow>?> = feature.queue
        .mapLatest(::rowsOf)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val waiting: StateFlow<EntryTranslateWaiting?> = feature.waiting
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val paused: StateFlow<Boolean> = feature.paused
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun pause() = feature.pause()

    fun resume() = feature.resume()

    fun cancelAll() {
        screenModelScope.launch { feature.cancelAll() }
    }

    fun cancel(row: TranslationQueueRow) {
        screenModelScope.launch { feature.cancel(listOf(row.chapter.id)) }
    }

    fun startNow(row: TranslationQueueRow) {
        screenModelScope.launch { feature.startNow(listOf(row.chapter.id)) }
    }

    /** Queues a failed chapter again; one that failed for want of setup opens the requirements first. */
    fun retry(row: TranslationQueueRow) {
        val chapter = TranslatableChapter(row.entry, row.chapter, downloadState(row.entry, row.chapter))
        translation.run(listOf(chapter), ChapterTranslateAction.RETRY, mapOf(row.chapter.id to row.status))
    }

    private suspend fun rowsOf(queue: List<EntryTranslateQueueItem>): List<TranslationQueueRow> {
        val entriesById = entries.getEntriesByIds(queue.map { it.entryId }.distinct()).associateBy(Entry::id)
        return queue.mapNotNull { item ->
            val entry = entriesById[item.entryId] ?: return@mapNotNull null
            val chapter = chapters.getChapterById(item.chapterId) ?: return@mapNotNull null
            TranslationQueueRow(entry, chapter, item.status)
        }
    }

    private fun downloadState(entry: Entry, chapter: EntryChapter): EntryDownloadState =
        downloads.status(
            type = entry.type,
            childId = chapter.id,
            childName = chapter.name,
            childScanlator = chapter.scanlator,
            childUrl = chapter.url,
            entryTitle = entry.title,
            sourceId = entry.source,
        )?.state ?: EntryDownloadState.NOT_DOWNLOADED

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
