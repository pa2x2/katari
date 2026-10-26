package eu.kanade.tachiyomi.ui.entry.downloads

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryDownloadChoices
import eu.kanade.presentation.entry.components.toChoices
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterList
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterSelection
import eu.kanade.tachiyomi.ui.entry.library.EntryLibraryModel
import eu.kanade.tachiyomi.ui.entry.members.EntryMembers
import eu.kanade.tachiyomi.ui.entry.olderchapters.EntryOlderChapters
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.download.EntryBulkDownloadAction
import mihon.entry.interactions.download.EntryBulkDownloadRequest
import mihon.entry.interactions.download.EntryBulkDownloadResolutionResult
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadActionRequest
import mihon.entry.interactions.download.EntryDownloadCancellationResult
import mihon.entry.interactions.download.EntryDownloadOptionSelection
import mihon.entry.interactions.download.EntryDownloadOptions
import mihon.entry.interactions.download.EntryDownloadOptionsFeature
import mihon.entry.interactions.download.EntryDownloadOptionsResolution
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import mihon.entry.interactions.download.sampleProgress
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.source.local.isLocal
import kotlin.time.Duration.Companion.milliseconds

/**
 * The downloads of an entry screen's chapters: their states and progress on the list, queueing them, through the
 * download options sheet when the entry's type has options, and cancelling or deleting them.
 */
@AssistedInject
class EntryChapterDownloads(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val context: Context,
    @Assisted private val snackbarHostState: SnackbarHostState,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val members: EntryMembers,
    @Assisted private val selection: EntryChapterSelection,
    @Assisted private val library: EntryLibraryModel,
    private val downloadRuntime: EntryDownloadRuntimeFeature,
    private val entryDownloadActionFeature: EntryDownloadActionFeature,
    private val entryDownloadOptionsFeature: EntryDownloadOptionsFeature,
    private val entryRepository: EntryRepository,
    private val olderChapters: EntryOlderChapters,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            context: Context,
            snackbarHostState: SnackbarHostState,
            state: EntryStateAccess,
            members: EntryMembers,
            selection: EntryChapterSelection,
            library: EntryLibraryModel,
        ): EntryChapterDownloads
    }

    private val optionsSheetState = MutableStateFlow<EntryDownloadOptionsSheet?>(null)
    val optionsSheet: StateFlow<EntryDownloadOptionsSheet?> = optionsSheetState.asStateFlow()

    private var optionsReloadJob: Job? = null

    // Progress ticks often, so only the download indicators that read it recompose instead of every tick rebuilding
    // the chapter list
    private val downloadProgress = mutableStateMapOf<Long, Int>()

    private val isFavorited: Boolean
        get() = state.loaded?.entry?.favorite ?: false

    init {
        scope.launchIO {
            downloadRuntime.statusUpdates()
                .filter { download ->
                    state.loaded?.containsChapter(download) == true
                }
                // Each tick looks up its chapter row, and the progress shown needn't move more often than this
                .sampleProgress(200.milliseconds)
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .collect(::updateDownloadState)
        }
    }

    fun progress(chapterId: Long): Int = downloadProgress[chapterId] ?: 0

    /** [chapters] of [entry] or its members as list items, with their download states. */
    suspend fun chapterItems(chapters: List<EntryChapter>, entry: Entry): List<EntryChapterList.Item> {
        val entriesById = mutableMapOf(entry.id to entry)
        val downloadStatuses = mutableMapOf<Long, EntryDownloadStatus>()
        chapters.groupBy { it.entryId }.forEach { (entryId, memberChapters) ->
            val chapterEntry = entriesById[entryId]
                ?: (entryRepository.getEntryById(entryId) ?: entry).also { entriesById[entryId] = it }
            if (!chapterEntry.isLocal()) {
                downloadRuntime.statuses(chapterEntry, memberChapters)?.let(downloadStatuses::putAll)
            }
        }
        return chapters.map { chapter ->
            val chapterEntry = entriesById.getValue(chapter.entryId)
            val isLocal = chapterEntry.isLocal()
            val activeDownload = if (isLocal) {
                null
            } else {
                downloadStatuses[chapter.id]
                    ?: EntryDownloadStatus(chapterEntry.type, chapter.id, EntryDownloadState.NOT_DOWNLOADED)
            }
            val downloadState = when {
                isLocal -> EntryDownloadState.DOWNLOADED
                activeDownload != null -> activeDownload.state
                else -> EntryDownloadState.NOT_DOWNLOADED
            }
            activeDownload?.let(::updateDownloadProgress)

            EntryChapterList.Item(
                chapter = chapter,
                entry = chapterEntry,
                downloadState = downloadState,
                selected = selection.isSelected(chapter.id),
            )
        }.toPersistentList()
    }

    fun start(items: List<EntryChapterList.Item>, startNow: Boolean) {
        val loaded = state.loaded ?: return
        if (items.isEmpty()) return

        if (!entryDownloadOptionsFeature.isApplicable(loaded.entry.type)) {
            queue(items, startNow)
            return
        }

        optionsSheetState.value = EntryDownloadOptionsSheet(items = items, startNow = startNow, choices = null)
        scope.launchIO {
            when (
                val resolution = entryDownloadOptionsFeature.resolve(
                    context,
                    loaded.entry,
                    items.first().chapter,
                    selection = null,
                )
            ) {
                is EntryDownloadOptionsResolution.Resolved -> optionsSheetState.update { sheet ->
                    if (sheet == null || sheet.items.map { it.id } != items.map { it.id }) return@update sheet
                    sheet.copy(choices = resolution.options.toChoices())
                }
                EntryDownloadOptionsResolution.ContextuallyUnavailable,
                EntryDownloadOptionsResolution.Inapplicable,
                -> {
                    var stillCurrent = false
                    optionsSheetState.update { sheet ->
                        if (sheet == null || sheet.items.map { it.id } != items.map { it.id }) return@update sheet
                        stillCurrent = true
                        null
                    }
                    if (stillCurrent) queue(items, startNow)
                }
            }
        }
    }

    fun selectOption(groupKey: String, optionKey: String?) {
        val entry = state.loaded?.entry ?: return
        var listing: Pair<EntryDownloadOptionsSheet, EntryDownloadChoices.Reload>? = null
        optionsSheetState.update { sheet ->
            val choices = sheet?.choices ?: return@update sheet
            val picked = choices.pick(groupKey, optionKey)
            listing = picked.reload?.takeIf { it != choices.reload }?.let { sheet to it }
            sheet.copy(choices = picked)
        }
        val (sheet, reload) = listing ?: return
        optionsReloadJob?.cancel()
        optionsReloadJob = scope.launchIO {
            val resolution = entryDownloadOptionsFeature.resolve(
                context,
                entry,
                sheet.items.first().chapter,
                EntryDownloadOptionSelection(reload.picks),
            )
            // The sheet opened with options, so getting none now means the source couldn't list them.
            val options = (resolution as? EntryDownloadOptionsResolution.Resolved)?.options
                ?: EntryDownloadOptions(groups = emptyList(), listingFailed = true)
            optionsSheetState.update { current ->
                val choices = current?.choices
                if (current?.items != sheet.items || choices?.reload != reload) return@update current
                current.copy(choices = choices.reloaded(options))
            }
        }
    }

    fun confirmOptions() {
        val sheet = optionsSheetState.value ?: return
        val choices = sheet.choices?.takeIf { it.canDownload } ?: return
        dismissOptions()
        queue(sheet.items, sheet.startNow, choices.selection)
    }

    fun dismissOptions() {
        optionsSheetState.value = null
    }

    fun runChapterActions(
        items: List<EntryChapterList.Item>,
        action: ChapterDownloadAction,
    ) {
        when (action) {
            ChapterDownloadAction.START -> {
                start(items, false)
                if (items.any { it.downloadState == EntryDownloadState.ERROR }) {
                    entryDownloadActionFeature.retry(items.map { EntryDownloadActionRequest.forEntry(it.entry) })
                }
            }
            ChapterDownloadAction.START_NOW -> {
                start(items, true)
            }
            ChapterDownloadAction.CANCEL -> {
                val chapterId = items.singleOrNull()?.id ?: return
                cancel(chapterId)
            }
            ChapterDownloadAction.DELETE -> {
                delete(items.map { it.chapter })
            }
        }
    }

    fun runBulkAction(action: DownloadAction) {
        val bulkAction = action.toEntryBulkDownloadAction()
        scope.launchNonCancellable {
            if (bulkAction.needsWholeChildList && state.loaded?.olderChapters != null) finishOlderChapters()
            val loaded = state.loaded ?: return@launchNonCancellable
            val candidateItems = loaded.processedChapters
                .filter { item -> item.downloadState == EntryDownloadState.NOT_DOWNLOADED }
            val result = entryDownloadActionFeature.resolveBulkDownloadCandidates(
                EntryBulkDownloadRequest(
                    entry = loaded.entry,
                    action = bulkAction,
                    sourceIds = loaded.chapters.mapTo(mutableSetOf(loaded.entry.source)) { it.entry.source },
                    visibleCandidates = candidateItems.map(EntryChapterList.Item::chapter),
                    memberEntryIds = loaded.memberIds,
                ),
            )
            if (result is EntryBulkDownloadResolutionResult.Candidates) {
                val itemsByChapterId = candidateItems.associateBy { it.chapter.id }
                start(result.chapters.mapNotNull { itemsByChapterId[it.id] }, false)
            }
        }
    }

    fun delete(chapters: List<EntryChapter>) {
        scope.launchNonCancellable {
            try {
                chapters.groupBy { it.entryId }
                    .forEach { (memberEntryId, memberChapters) ->
                        val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                        entryDownloadActionFeature.delete(entry, memberChapters)
                    }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    private fun queue(
        items: List<EntryChapterList.Item>,
        startNow: Boolean,
        optionSelection: EntryDownloadOptionSelection? = null,
    ) {
        val loaded = state.loaded ?: return
        selection.setAll(false)

        scope.launchNonCancellable {
            val entry = loaded.entry
            val chapters = items.map { it.chapter }
            if (optionSelection != null) {
                entryDownloadOptionsFeature.download(entry, chapters, optionSelection, startNow)
            } else {
                entryDownloadActionFeature.download(entry, chapters, startNow)
            }

            if (!isFavorited && !loaded.hasPromptedToAddBefore) {
                state.update { it.copy(hasPromptedToAddBefore = true) }
                val result = snackbarHostState.showSnackbar(
                    message = context.stringResource(MR.strings.snack_add_to_library),
                    actionLabel = context.stringResource(MR.strings.action_add),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed && !isFavorited) {
                    library.toggleFavorite()
                }
            }
        }
    }

    private fun cancel(chapterId: Long) {
        val chapterItem = state.loaded?.chapters.orEmpty().firstOrNull { it.id == chapterId } ?: return
        val result = entryDownloadActionFeature.cancel(
            EntryDownloadActionRequest.forEntry(chapterItem.entry),
            chapterId,
        )
        if (result is EntryDownloadCancellationResult.Cancelled) {
            updateDownloadState(result.status)
        }
    }

    // Reads the rest of the chapter lists, then the chapters from storage, so candidates come from every chapter rather
    // than waiting for the list's own update
    private suspend fun finishOlderChapters() {
        val loaded = state.loaded ?: return
        olderChapters.finish(loaded.chapters.map(EntryChapterList.Item::entry).plus(loaded.entry).distinctBy(Entry::id))
        val entry = runCatching { entryRepository.getEntryById(loaded.entry.id) }.getOrNull() ?: return
        val chapters = chapterItems(members.displayedChapters(), entry)
        state.update { it.copy(entry = entry, chapters = chapters).rebuildChapterPresentation() }
    }

    private fun updateDownloadProgress(download: EntryDownloadStatus) {
        if (download.state == EntryDownloadState.DOWNLOADING) {
            downloadProgress[download.chapterId] = download.progress
        } else {
            downloadProgress.remove(download.chapterId)
        }
    }

    private fun updateDownloadState(download: EntryDownloadStatus) {
        updateDownloadProgress(download)
        state.update { loaded ->
            val item = loaded.chapter(download) ?: return@update loaded
            if (item.downloadState == download.state) return@update loaded
            val updatedItem = item.copy(downloadState = download.state)
            loaded.updateChapterItems(
                items = listOf(updatedItem),
                rebuildPresentation = loaded.entry.downloadedFilter != TriState.DISABLED &&
                    item.isDownloaded != updatedItem.isDownloaded,
            )
        }
    }
}

/** The download options sheet for [items]; [choices] is null until the sheet's options are listed. */
data class EntryDownloadOptionsSheet(
    val items: List<EntryChapterList.Item>,
    val startNow: Boolean,
    val choices: EntryDownloadChoices?,
)

private fun DownloadAction.toEntryBulkDownloadAction(): EntryBulkDownloadAction {
    return when (this) {
        DownloadAction.NEXT_1_CHAPTER -> EntryBulkDownloadAction.next(1)
        DownloadAction.NEXT_5_CHAPTERS -> EntryBulkDownloadAction.next(5)
        DownloadAction.NEXT_10_CHAPTERS -> EntryBulkDownloadAction.next(10)
        DownloadAction.NEXT_25_CHAPTERS -> EntryBulkDownloadAction.next(25)
        DownloadAction.UNREAD_CHAPTERS -> EntryBulkDownloadAction.unread
        DownloadAction.BOOKMARKED_CHAPTERS -> EntryBulkDownloadAction.bookmarked
    }
}
