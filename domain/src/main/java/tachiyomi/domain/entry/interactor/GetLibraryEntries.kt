package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retry
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryLibraryGroupResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressResolutionPort
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Duration.Companion.seconds

class GetLibraryEntries(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryLibraryProgressResolver: EntryLibraryProgressResolutionPort,
    private val categoryRepository: CategoryRepository,
    private val libraryGrouping: EntryLibraryGroupingResolutionPort,
    private val hiddenSourceIds: HiddenSourceIds,
    private val sourceManager: SourceManager,
    private val sourceDescription: EntrySourceDescriptionResolutionPort,
) {

    suspend fun await(): List<LibraryItem> {
        val favorites = entryRepository.getLibraryEntries()
        if (favorites.isEmpty()) return emptyList()
        val profileId = favorites.first().profileId
        return buildItems(
            profileId = profileId,
            favorites = favorites,
            groups = libraryGrouping.resolveLibraryGrouping(profileId, favorites).groups,
            hiddenSources = hiddenSourceIds.get(profileId),
        )
    }

    fun subscribe(): Flow<List<LibraryItem>> {
        return observeEntries(entryRepository.getLibraryEntriesAsFlow())
    }

    fun subscribe(profileId: Long): Flow<List<LibraryItem>> {
        return observeEntries(
            entries = entryRepository.getLibraryEntriesAsFlow(profileId),
            expectedProfileId = profileId,
        )
    }

    private fun observeEntries(
        entries: Flow<List<Entry>>,
        expectedProfileId: Long? = null,
    ): Flow<List<LibraryItem>> {
        return entries
            .flatMapLatest { favorites ->
                if (favorites.isEmpty()) return@flatMapLatest flowOf(emptyList())
                val profileId = favorites.first().profileId
                check(expectedProfileId == null || profileId == expectedProfileId) {
                    "Library entries belong to profile $profileId instead of $expectedProfileId"
                }
                combine(
                    libraryGrouping.observeLibraryGrouping(profileId, flowOf(favorites)),
                    hiddenSourceIds.subscribe(profileId),
                ) { grouping, hiddenSources ->
                    buildItems(profileId, favorites, grouping.groups, hiddenSources)
                }
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
        profileId: Long,
        favorites: List<Entry>,
        groups: List<EntryLibraryGroupResolution>,
        hiddenSources: Set<Long>,
    ): List<LibraryItem> {
        if (favorites.isEmpty()) return emptyList()

        val entryIds = favorites.map { it.id }
        val chapters = entryChapterRepository.getChaptersByEntryIds(entryIds).first()
        val categoryIdsByEntryId = categoryRepository.getCategoryIdsByEntryIds(profileId, entryIds)
        val lastReadByEntryId = entryRepository.getLibraryLastRead(profileId)

        val itemsById = favorites.associate { entry ->
            val entryChapters = chapters.filter { it.entryId == entry.id }
            val libraryState = entryLibraryProgressResolver.calculate(
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

        val collapsedItems = groups.mapNotNull { group ->
            val members = group.orderedEntries.mapNotNull { entry -> itemsById[entry.id] }
            when {
                members.size > 1 -> mergeEntryItem(group.visibleEntry.id, members)
                members.size == 1 -> members.single()
                else -> null
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

        val memberSummaries = members.mapNotNull { it.availableProgressSummary }
        val libraryState = if (memberSummaries.size == members.size) {
            entryLibraryProgressResolver.merge(target.entry.type, memberSummaries)
        } else {
            EntryLibraryProgressResolution.Inapplicable(target.entry.type)
        }

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
            progressSummary = libraryState,
            latestUpload = members.maxOfOrNull { it.latestUpload } ?: 0L,
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
        libraryState: EntryLibraryProgressResolution,
    ): LibraryItem {
        val source = sourceManager.getOrStub(entry.source)
        val sourceDisplayInfo = sourceManager.getDisplayInfo(entry.source)
        val sourceName = sourceDisplayInfo.name
        val sourceLanguage = sourceDisplayInfo.lang
        val sourceItemOrientation = sourceDescription.describe(source).itemOrientation

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
            progressSummary = libraryState,
            latestUpload = chapters.maxOfOrNull { it.dateUpload }?.takeIf { it > 0 } ?: entry.lastUpdate,
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
