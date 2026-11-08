package eu.kanade.tachiyomi.ui.download.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.download.EntryDownloadActionFeature
import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.entry.interactions.download.EntryDownloadState
import app.katari.entry.interactions.translate.EntryTranslateFeature
import app.katari.entry.interactions.translate.EntryTranslateQueueItem
import app.katari.entry.interactions.translate.EntryTranslateWaiting
import app.katari.entry.interactions.translation.EntryTranslationLanguagesFeature
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import eu.kanade.presentation.entry.translation.TranslatableChapter
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.entry.translation.ChapterTranslationModel
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSetupHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

/** The translation queue as the queue screen lists it, with pausing, cancelling and retrying its chapters. */
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class TranslationQueueViewModel(
    context: PlatformContext,
    private val feature: EntryTranslateFeature,
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
    private val downloads: EntryDownloadRuntimeFeature,
    private val downloadAction: EntryDownloadActionFeature,
    languages: EntryTranslationLanguagesFeature,
    setupHost: EntryTranslateSetupHost,
) : ViewModel() {
    val snackbarHostState = SnackbarHostState()

    /** Whether any Entry type translates in the background, so the queue screen shows a translation tab at all. */
    val isAvailable: Boolean = EntryType.entries.any(feature::isApplicable)

    /** Settles what blocks chapters that failed for want of setup before they are queued again. */
    val translation = ChapterTranslationModel(
        scope = viewModelScope,
        context = context,
        snackbarHostState = snackbarHostState,
        download = { items ->
            viewModelScope.launch {
                items.groupBy { it.entry }.forEach { (entry, group) ->
                    downloadAction.download(entry, group.map { it.chapter }, startNow = false)
                }
            }
        },
        feature = feature,
        languages = languages,
        setupHost = setupHost,
    )

    /** The queue in processing order; null until first loaded. */
    val rows: StateFlow<List<TranslationQueueRow>?> = feature.queue
        .mapLatest(::rowsOf)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val waiting: StateFlow<EntryTranslateWaiting?> = feature.waiting
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val paused: StateFlow<Boolean> = feature.paused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun pause() = feature.pause()

    fun resume() = feature.resume()

    fun cancelAll() {
        viewModelScope.launch { feature.cancelAll() }
    }

    fun cancel(row: TranslationQueueRow) {
        viewModelScope.launch { feature.cancel(listOf(row.chapter.id)) }
    }

    fun startNow(row: TranslationQueueRow) {
        viewModelScope.launch { feature.startNow(listOf(row.chapter.id)) }
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
            entryTitle = entry.downloadFolderTitle,
            sourceId = entry.source,
        )?.state ?: EntryDownloadState.NOT_DOWNLOADED

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
