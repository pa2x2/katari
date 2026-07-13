package eu.kanade.tachiyomi.ui.updates

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastFilter
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.core.util.addOrRemove
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.updates.UpdatesSelectionState
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.presentation.updates.toUpdatesUiModels
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.collapseByVisibleEntry
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryConsumptionInteraction
import mihon.entry.interactions.EntryConsumptionStatus
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.model.toUpdateItem
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.domain.util.applyFilter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime

class UpdatesScreenModel(
    private val entryDownloadInteraction: EntryDownloadInteraction = Injekt.get(),
    private val entryConsumptionInteraction: EntryConsumptionInteraction = Injekt.get(),
    private val getUpdates: GetUpdates = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val getMergedEntry: GetMergedEntry = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val updatesPreferences: UpdatesPreferences = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<UpdatesScreenModel.State>(State()) {

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    val lastUpdated by libraryPreferences.lastUpdatedTimestamp.asState(screenModelScope)

    private val selectionState = UpdatesSelectionState()
    private val selectedKeys: HashSet<LibraryItemKey> = HashSet()

    init {
        screenModelScope.launchIO {
            // Set date limit for recent chapters/episodes
            val limit = ZonedDateTime.now().minusMonths(3).toInstant()

            combine(
                // needed for SQL filters (unread, started, bookmarked, etc)
                getUpdatesItemPreferenceFlow()
                    .distinctUntilChanged()
                    .flatMapLatest {
                        getUpdates.subscribe(
                            limit,
                            unread = it.filterUnread.toBooleanOrNull(),
                            started = it.filterStarted.toBooleanOrNull(),
                            bookmarked = it.filterBookmarked.toBooleanOrNull(),
                            hideExcludedScanlators = it.filterExcludedScanlators,
                        ).distinctUntilChanged()
                    },
                entryDownloadInteraction.changes,
                // needed for Kotlin filters (downloaded)
                getUpdatesItemPreferenceFlow().distinctUntilChanged { old, new ->
                    old.filterDownloaded == new.filterDownloaded
                },
            ) { updates, _, itemPreferences ->
                Triple(updates, itemPreferences, Unit)
            }
                .collectLatest { (updates, itemPreferences, _) ->
                    val updateItems = updates
                        .toUpdateItems()
                        .applyFilters(itemPreferences)
                        .collapseByVisibleEntry(
                            actualEntryId = { it.update.entryId },
                            visibleEntryId = { it.visibleEntryId },
                        )
                        .toPersistentList()
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = updateItems,
                        )
                    }
                }
        }

        screenModelScope.launchIO {
            entryDownloadInteraction.updates()
                .catch { logcat(LogPriority.ERROR, it) }
                .collect(this@UpdatesScreenModel::updateDownloadState)
        }

        getUpdatesItemPreferenceFlow()
            .map { prefs ->
                listOf(
                    prefs.filterUnread,
                    prefs.filterDownloaded,
                    prefs.filterStarted,
                    prefs.filterBookmarked,
                )
                    .any { it != TriState.DISABLED }
            }
            .distinctUntilChanged()
            .onEach {
                mutableState.update { state ->
                    state.copy(hasActiveFilters = it)
                }
            }
            .launchIn(screenModelScope)
    }

    private fun List<UpdatesItem>.applyFilters(
        preferences: ItemPreferences,
    ): List<UpdatesItem> {
        val filterDownloaded = preferences.filterDownloaded

        val filterFnDownloaded: (UpdatesItem) -> Boolean = {
            applyFilter(filterDownloaded) {
                it.update is UpdateItem.EntryUpdate &&
                    it.downloadStateProvider() == EntryDownloadState.DOWNLOADED
            }
        }

        return fastFilter {
            filterFnDownloaded(it)
        }
    }

    private suspend fun List<UpdatesWithRelations>.toUpdateItems(): List<UpdatesItem> {
        val visibleTargetCache = mutableMapOf<Long, Long>()
        val entryCache = mutableMapOf<Long, tachiyomi.domain.entry.model.Entry?>()

        return map { updateWithRelations ->
            val update = updateWithRelations.toUpdateItem(updateWithRelations.entryType)

            val visibleEntryId = visibleTargetCache.getOrPut(update.entryId) {
                getMergedEntry.awaitVisibleTargetId(update.entryId)
            }
            val entry = entryCache.getOrPut(visibleEntryId) {
                getEntry.await(visibleEntryId)
            }

            when (update) {
                is UpdateItem.EntryUpdate -> {
                    val chapterUpdate = update.update
                    val downloadStatus = entryDownloadInteraction.getStatus(
                        entryType = update.entryType,
                        chapterId = chapterUpdate.chapterId,
                        chapterName = chapterUpdate.chapterName,
                        chapterScanlator = chapterUpdate.scanlator,
                        chapterUrl = chapterUpdate.chapterUrl,
                        entryTitle = chapterUpdate.entryTitle,
                        sourceId = chapterUpdate.sourceId,
                    )
                    UpdatesItem(
                        update = update,
                        visibleEntryId = visibleEntryId,
                        visibleEntryTitle = entry?.displayTitle ?: chapterUpdate.entryTitle,
                        visibleCoverData = entry?.asEntryCover() ?: chapterUpdate.coverData,
                        downloadStateProvider = { downloadStatus.state },
                        downloadProgressProvider = { downloadStatus.progress },
                        selected = update.key in selectedKeys,
                    )
                }
            }
        }
    }

    fun updateLibrary(): Boolean {
        val started = LibraryUpdateJob.startNow(Injekt.get<Application>())
        screenModelScope.launch {
            _events.send(Event.LibraryUpdateTriggered(started))
        }
        return started
    }

    /**
     * Update status of downloads.
     *
     * @param download download object containing progress.
     */
    private fun updateDownloadState(download: EntryDownloadStatus) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().also { list ->
                val modifiedIndex = list.indexOfFirst {
                    it.update is UpdateItem.EntryUpdate &&
                        it.update.entryType == download.entryType &&
                        it.update.update.chapterId == download.chapterId
                }
                if (modifiedIndex < 0) return@also

                val item = list[modifiedIndex]
                list[modifiedIndex] = item.copy(
                    downloadStateProvider = { download.state },
                    downloadProgressProvider = { download.progress },
                )
            }
            state.copy(items = newItems)
        }
    }

    fun downloadChapters(items: List<UpdatesItem>, action: ChapterDownloadAction) {
        val chapterItems = items.filter { it.update is UpdateItem.EntryUpdate }
        if (chapterItems.isEmpty()) return
        screenModelScope.launch {
            when (action) {
                ChapterDownloadAction.START -> {
                    downloadChapters(chapterItems)
                    if (chapterItems.any { it.downloadStateProvider() == EntryDownloadState.ERROR }) {
                        entryDownloadInteraction.startDownloads()
                    }
                }
                ChapterDownloadAction.START_NOW -> {
                    val chapterUpdate = chapterItems.singleOrNull()
                        ?.let { it.update as UpdateItem.EntryUpdate }
                        ?: return@launch
                    val entry = getEntry.await(chapterUpdate.update.entryId) ?: return@launch
                    val chapter = entryChapterRepository.getChapterById(chapterUpdate.update.chapterId)
                        ?: return@launch
                    entryDownloadInteraction.download(entry, listOf(chapter), startNow = true)
                }
                ChapterDownloadAction.CANCEL -> {
                    val update = chapterItems.singleOrNull()
                        ?.let { it.update as UpdateItem.EntryUpdate }
                        ?: return@launch
                    cancelDownload(update)
                }
                ChapterDownloadAction.DELETE -> {
                    deleteChapters(chapterItems)
                }
            }
            toggleAllSelection(false)
        }
    }

    private fun cancelDownload(update: UpdateItem.EntryUpdate) {
        val downloadStatus = entryDownloadInteraction.cancelQueuedDownload(
            entryType = update.entryType,
            chapterId = update.update.chapterId,
        ) ?: return
        updateDownloadState(downloadStatus)
    }

    fun markUpdatesConsumed(updates: List<UpdatesItem>, consumed: Boolean) {
        screenModelScope.launchNonCancellable {
            updates.entryChapterSelections()
                .forEach { (entry, chapters) ->
                    entryConsumptionInteraction.setConsumed(entry, chapters, consumed)
                }
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param updates the list of chapters to bookmark.
     */
    fun bookmarkUpdates(updates: List<UpdatesItem>, bookmark: Boolean) {
        screenModelScope.launchNonCancellable {
            updates.entryChapterSelections()
                .forEach { (entry, chapters) ->
                    entryConsumptionInteraction.setBookmarked(entry, chapters, bookmark)
                }
        }
        toggleAllSelection(false)
    }

    fun hasBookmarkAction(updates: List<UpdatesItem>, bookmark: Boolean): Boolean {
        return updates.hasBookmarkAction(
            bookmark = bookmark,
            supportsBookmark = entryConsumptionInteraction::supportsBookmark,
            canSetBookmarked = entryConsumptionInteraction::canSetBookmarked,
        )
    }

    fun hasConsumedAction(updates: List<UpdatesItem>, consumed: Boolean): Boolean {
        return updates.hasConsumedAction(
            consumed = consumed,
            canSetConsumed = entryConsumptionInteraction::canSetConsumed,
        )
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param updatesItem the list of chapters to download.
     */
    private fun downloadChapters(updatesItem: List<UpdatesItem>) {
        screenModelScope.launchNonCancellable {
            val chapterUpdates = updatesItem.mapNotNull { it.update as? UpdateItem.EntryUpdate }
            val groupedUpdates = chapterUpdates.groupBy { it.update.entryId }.values
            for (updates in groupedUpdates) {
                val entryId = updates.first().update.entryId
                val entry = getEntry.await(entryId) ?: continue
                val chapters = updates.mapNotNull { entryChapterRepository.getChapterById(it.update.chapterId) }
                entryDownloadInteraction.download(entry, chapters)
            }
        }
    }

    /**
     * Delete selected chapters
     *
     * @param updatesItem list of chapters
     */
    fun deleteChapters(updatesItem: List<UpdatesItem>) {
        screenModelScope.launchNonCancellable {
            val chapterUpdates = updatesItem.mapNotNull { it.update as? UpdateItem.EntryUpdate }
            chapterUpdates
                .groupBy { it.update.entryId }
                .entries
                .forEach { (entryId, updates) ->
                    val entry = getEntry.await(entryId) ?: return@forEach
                    val chapters = updates.mapNotNull { entryChapterRepository.getChapterById(it.update.chapterId) }
                    entryDownloadInteraction.delete(entry, chapters)
                }
        }
        toggleAllSelection(false)
    }

    fun showConfirmDeleteChapters(updatesItem: List<UpdatesItem>) {
        setDialog(Dialog.DeleteConfirmation(updatesItem))
    }

    fun toggleSelection(
        item: UpdatesItem,
        selected: Boolean,
        fromLongPress: Boolean = false,
    ) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val selectedIndex = indexOfFirst { it.update.key == item.update.key }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if (selectedItem.selected == selected) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedKeys.addOrRemove(item.update.key, selected)

                if (selected && fromLongPress) {
                    selectionState.updateRangeSelection(selectedIndex, firstSelection).forEach {
                        val inbetweenItem = get(it)
                        if (!inbetweenItem.selected) {
                            selectedKeys.add(inbetweenItem.update.key)
                            set(it, inbetweenItem.copy(selected = true))
                        }
                    }
                } else if (!fromLongPress) {
                    selectionState.updateSelectionBounds(
                        selectedIndex = selectedIndex,
                        selected = selected,
                        firstSelectedIndex = indexOfFirst { it.selected },
                        lastSelectedIndex = indexOfLast { it.selected },
                    )
                }
            }
            state.copy(items = newItems)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedKeys.addOrRemove(it.update.key, selected)
                it.copy(selected = selected)
            }
            state.copy(items = newItems)
        }

        selectionState.reset()
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedKeys.addOrRemove(it.update.key, !it.selected)
                it.copy(selected = !it.selected)
            }
            state.copy(items = newItems)
        }
        selectionState.reset()
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun resetNewUpdatesCount() {
        libraryPreferences.newUpdatesCount.set(0)
    }

    private fun getUpdatesItemPreferenceFlow(): Flow<ItemPreferences> {
        return combine(
            updatesPreferences.filterDownloaded.changes(),
            updatesPreferences.filterUnread.changes(),
            updatesPreferences.filterStarted.changes(),
            updatesPreferences.filterBookmarked.changes(),
            updatesPreferences.filterExcludedScanlators.changes(),
        ) { downloaded, unread, started, bookmarked, excludedScanlators ->
            ItemPreferences(
                filterDownloaded = downloaded,
                filterUnread = unread,
                filterStarted = started,
                filterBookmarked = bookmarked,
                filterExcludedScanlators = excludedScanlators,
            )
        }
    }

    fun showFilterDialog() {
        mutableState.update { it.copy(dialog = Dialog.FilterSheet) }
    }

    @Immutable
    private data class ItemPreferences(
        val filterDownloaded: TriState,
        val filterUnread: TriState,
        val filterStarted: TriState,
        val filterBookmarked: TriState,
        val filterExcludedScanlators: Boolean,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val hasActiveFilters: Boolean = false,
        val items: List<UpdatesItem> = listOf(),
        val dialog: Dialog? = null,
    ) {
        val selected = items.filter { it.selected }
        val selectionMode = selected.isNotEmpty()

        fun getUiModel(): List<UpdatesUiModel<UpdatesItem>> {
            return items.toUpdatesUiModels { it.update.dateFetch.toLocalDate() }
        }
    }

    sealed interface Dialog {
        data class DeleteConfirmation(val toDelete: List<UpdatesItem>) : Dialog
        data object FilterSheet : Dialog
    }

    sealed interface Event {
        data object InternalError : Event
        data class LibraryUpdateTriggered(val started: Boolean) : Event
    }

    private suspend fun List<UpdatesItem>.entryChapterSelections(): List<EntryChapterSelection> {
        return mapNotNull { it.update as? UpdateItem.EntryUpdate }
            .groupBy { it.update.entryId }
            .mapNotNull { (entryId, updates) ->
                val entry = getEntry.await(entryId) ?: return@mapNotNull null
                val chapters = updates.mapNotNull { entryChapterRepository.getChapterById(it.update.chapterId) }
                EntryChapterSelection(entry, chapters)
            }
            .filter { it.chapters.isNotEmpty() }
    }

    private data class EntryChapterSelection(
        val entry: Entry,
        val chapters: List<EntryChapter>,
    )
}

private fun TriState.toBooleanOrNull(): Boolean? {
    return when (this) {
        TriState.DISABLED -> null
        TriState.ENABLED_IS -> true
        TriState.ENABLED_NOT -> false
    }
}

@Immutable
data class UpdatesItem(
    val update: UpdateItem,
    val visibleEntryId: Long,
    val visibleEntryTitle: String,
    val visibleCoverData: EntryCover,
    val downloadStateProvider: () -> EntryDownloadState,
    val downloadProgressProvider: () -> Int,
    val selected: Boolean = false,
)

internal fun List<UpdatesItem>.hasBookmarkAction(
    bookmark: Boolean,
    supportsBookmark: (EntryType) -> Boolean,
    canSetBookmarked: (entryType: EntryType, status: EntryConsumptionStatus, bookmarked: Boolean) -> Boolean,
): Boolean {
    val supportedUpdates = mapNotNull { it.update as? UpdateItem.EntryUpdate }
        .filter { supportsBookmark(it.entryType) }

    return supportedUpdates.isNotEmpty() &&
        supportedUpdates.size == size &&
        when (bookmark) {
            true -> supportedUpdates.any { canSetBookmarked(it.entryType, it.consumptionStatus(), true) }
            false -> supportedUpdates.all { canSetBookmarked(it.entryType, it.consumptionStatus(), false) }
        }
}

internal fun List<UpdatesItem>.hasConsumedAction(
    consumed: Boolean,
    canSetConsumed: (entryType: EntryType, status: EntryConsumptionStatus, consumed: Boolean) -> Boolean,
): Boolean {
    return mapNotNull { it.update as? UpdateItem.EntryUpdate }
        .any { canSetConsumed(it.entryType, it.consumptionStatus(), consumed) }
}

private fun UpdateItem.EntryUpdate.consumptionStatus(): EntryConsumptionStatus {
    return EntryConsumptionStatus(
        consumed = update.read,
        bookmarked = update.bookmark,
        hasPartialProgress = !update.read && update.started,
    )
}
