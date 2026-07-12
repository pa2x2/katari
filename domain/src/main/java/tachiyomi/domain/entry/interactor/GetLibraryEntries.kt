package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.entryItemOrientation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retry
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressResolver
import tachiyomi.domain.entry.service.EntryLibraryState
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Duration.Companion.seconds

class GetLibraryEntries(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryLibraryProgressResolver: EntryLibraryProgressResolver,
    private val categoryRepository: CategoryRepository,
    private val mergedEntryRepository: MergedEntryRepository,
    private val hiddenSourceIds: HiddenSourceIds,
    private val sourceManager: SourceManager,
) {

    suspend fun await(): List<LibraryItem> {
        return buildItems(
            favorites = entryRepository.getLibraryEntries(),
            merges = mergedEntryRepository.getAll(),
            hiddenSources = hiddenSourceIds.get(),
        )
    }

    fun subscribe(): Flow<List<LibraryItem>> {
        return combine(
            entryRepository.getLibraryEntriesAsFlow(),
            mergedEntryRepository.subscribeAll(),
            hiddenSourceIds.subscribe(),
        ) { favorites, merges, hiddenSources ->
            buildItems(favorites, merges, hiddenSources)
        }
            .retry {
                if (it is NullPointerException) {
                    delay(0.5.seconds)
                    true
                } else {
                    false
                }
            }
            .catch {
                this@GetLibraryEntries.logcat(LogPriority.ERROR, it)
            }
    }

    private suspend fun buildItems(
        favorites: List<Entry>,
        merges: List<EntryMerge>,
        hiddenSources: Set<Long>,
    ): List<LibraryItem> {
        if (favorites.isEmpty()) return emptyList()

        val entryIds = favorites.map { it.id }
        val chapters = entryChapterRepository.getChaptersByEntryIds(entryIds).first()
        val categoryIdsByEntryId = categoryRepository.getCategoryIdsByEntryIds(entryIds)
        val lastReadByEntryId = entryRepository.getLibraryLastRead()

        val itemsById = favorites.associate { entry ->
            val entryChapters = chapters.filter { it.entryId == entry.id }
            val libraryState = entryLibraryProgressResolver.resolve(
                entry = entry,
                chapters = entryChapters,
                lastRead = lastReadByEntryId[entry.id] ?: 0L,
            )

            entry.id to buildLibraryItem(
                entry = entry,
                memberEntries = listOf(entry),
                chapters = entryChapters,
                categories = categoryIdsByEntryId[entry.id].orDefaultCategory(),
                displaySourceId = entry.source,
                sourceIds = setOf(entry.source),
                isMerged = false,
                libraryState = libraryState,
            )
        }

        val mergesByTargetId = merges.groupBy { it.targetId }
        val mergeByEntryId = merges.associateBy { it.entryId }
        val collapsedItems = mutableListOf<LibraryItem>()
        val consumedIds = mutableSetOf<Long>()

        favorites.forEach { entry ->
            if (!consumedIds.add(entry.id)) return@forEach

            val targetId = mergeByEntryId[entry.id]?.targetId
            val members = targetId
                ?.let { mergesByTargetId[it] }
                .orEmpty()
                .sortedBy { it.position }
                .mapNotNull { itemsById[it.entryId] }

            if (members.size > 1) {
                collapsedItems += mergeEntryItem(targetId ?: entry.id, members)
                consumedIds += members.map { it.entry.id }
            } else {
                collapsedItems += itemsById.getValue(entry.id)
            }
        }

        return collapsedItems.filterNot { item ->
            val visibleSources = item.sourceIds - hiddenSources
            visibleSources.isEmpty()
        }
    }

    private fun mergeEntryItem(
        targetId: Long,
        members: List<LibraryItem>,
    ): LibraryItem {
        val target = members.firstOrNull { it.entry.id == targetId } ?: members.first()
        val sourceIds = members.flatMap { it.sourceIds }.toSet()
        val displaySourceId = if (sourceIds.size > 1) MULTI_SOURCE_ID else sourceIds.first()

        val sourceName = if (displaySourceId == MULTI_SOURCE_ID) {
            ""
        } else {
            members.firstOrNull { displaySourceId in it.sourceIds }?.sourceName.orEmpty()
        }
        val sourceLanguage = if (displaySourceId == MULTI_SOURCE_ID) {
            MULTI_SOURCE_ID.toString()
        } else {
            members.firstOrNull { displaySourceId in it.sourceIds }?.sourceLanguage.orEmpty()
        }
        val sourceItemOrientation = if (displaySourceId == MULTI_SOURCE_ID) {
            EntryItemOrientation.VERTICAL
        } else {
            members.firstOrNull { displaySourceId in it.sourceIds }?.sourceItemOrientation
                ?: EntryItemOrientation.VERTICAL
        }

        val libraryState = entryLibraryProgressResolver.merge(target.entry.type, members)

        return target.copy(
            categories = members.flatMap { it.categories }.distinct(),
            sourceName = sourceName,
            sourceLanguage = sourceLanguage,
            sourceItemOrientation = sourceItemOrientation,
            displaySourceId = displaySourceId,
            sourceIds = sourceIds,
            isMerged = true,
            memberEntryIds = members.flatMap { it.memberEntryIds },
            memberEntries = members.flatMap { it.memberEntries },
            progress = libraryState.progress,
            latestUpload = members.maxOfOrNull { it.latestUpload } ?: 0L,
            lastRead = libraryState.lastRead,
            continueEntryId = libraryState.continueEntryId,
        )
    }

    private fun buildLibraryItem(
        entry: Entry,
        memberEntries: List<Entry>,
        chapters: List<EntryChapter>,
        categories: List<Long>,
        displaySourceId: Long,
        sourceIds: Set<Long>,
        isMerged: Boolean,
        libraryState: EntryLibraryState,
    ): LibraryItem {
        val source = sourceManager.getOrStub(entry.source)
        val sourceDisplayInfo = sourceManager.getDisplayInfo(entry.source)
        val sourceName = sourceDisplayInfo.name
        val sourceLanguage = sourceDisplayInfo.lang
        val sourceItemOrientation = source.entryItemOrientation()

        return LibraryItem(
            entry = entry,
            categories = categories,
            sourceName = sourceName,
            sourceLanguage = sourceLanguage,
            sourceItemOrientation = sourceItemOrientation,
            displaySourceId = displaySourceId,
            sourceIds = sourceIds,
            isLocal = false,
            isMerged = isMerged,
            memberEntryIds = memberEntries.map { LibraryItemKey(entry.type, it.id) },
            memberEntries = memberEntries,
            progress = libraryState.progress,
            latestUpload = chapters.maxOfOrNull { it.dateUpload }?.takeIf { it > 0 } ?: entry.lastUpdate,
            lastRead = libraryState.lastRead,
            continueEntryId = libraryState.continueEntryId,
            downloadCount = 0,
        )
    }

    private fun List<Long>?.orDefaultCategory(): List<Long> {
        return this?.ifEmpty { listOf(Category.UNCATEGORIZED_ID) }
            ?: listOf(Category.UNCATEGORIZED_ID)
    }

    companion object {
        private const val MULTI_SOURCE_ID = Long.MIN_VALUE
    }
}
