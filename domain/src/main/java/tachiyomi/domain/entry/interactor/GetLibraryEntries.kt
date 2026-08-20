package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retry
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.category.repository.LibraryCategoryMappingObserver
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.LibraryLastReadObserver
import tachiyomi.domain.entry.service.EntryLibraryGroupResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryProgressMember
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressResolutionPort
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
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
            groups = libraryGrouping.resolveLibraryGrouping(profileId, favorites).groups.map { it.toIdentity() },
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
        return observeLibraryFavoriteSets(entries, expectedProfileId)
            .flatMapLatest { observation ->
                when (observation) {
                    LibraryFavoriteSetObservation.Empty -> flow {
                        delay(LIBRARY_INVALIDATION_DEBOUNCE)
                        emit(emptyList())
                    }
                    is LibraryFavoriteSetObservation.Populated -> observeMembership(observation)
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

    private fun observeMembership(
        observation: LibraryFavoriteSetObservation.Populated,
    ): Flow<List<LibraryItem>> = flow {
        delay(LIBRARY_INVALIDATION_DEBOUNCE)
        val initialFavorites = observation.updates.receive()
        val favorites = flow {
            emit(initialFavorites)
            emitAll(observation.updates.receiveAsFlow())
        }
        val profileId = observation.key.profileId
        val entryIds = observation.key.orderedEntryIds
        val details = combine(
            entryChapterRepository.getChaptersByEntryIds(entryIds),
            observeCategoryIds(profileId, entryIds),
            observeLibraryLastRead(profileId),
        ) { chapters, categories, lastRead ->
            LibraryDetailInput(
                chapters = chapters,
                categoryIdsByEntryId = categories,
                lastReadByEntryId = lastRead,
            )
        }.distinctUntilChanged()
        var previousInput: LibraryBuildInput? = null
        var previousItems: List<LibraryItem>? = null
        emitAll(
            combine(
                favorites,
                libraryGrouping.observeLibraryGrouping(profileId, flowOf(initialFavorites))
                    .map { grouping -> grouping.groups.map { it.toIdentity() } }
                    .distinctUntilChanged(),
                hiddenSourceIds.subscribe(profileId),
                details,
            ) { currentFavorites, groups, hiddenSources, detailInput ->
                LibraryBuildInput(
                    favorites = currentFavorites,
                    groups = groups,
                    hiddenSources = hiddenSources,
                    chapters = detailInput.chapters,
                    categoryIdsByEntryId = detailInput.categoryIdsByEntryId,
                    lastReadByEntryId = detailInput.lastReadByEntryId,
                )
            }
                .debounce(LIBRARY_INVALIDATION_DEBOUNCE)
                .mapLatest { input ->
                    val items = previousInput
                        ?.takeIf(input::isPinOnlyUpdateFrom)
                        ?.let { previousItems?.applyEntryUpdates(input.favorites) }
                        ?: buildItems(
                            favorites = input.favorites,
                            groups = input.groups,
                            hiddenSources = input.hiddenSources,
                            chapters = input.chapters,
                            categoryIdsByEntryId = input.categoryIdsByEntryId,
                            lastReadByEntryId = input.lastReadByEntryId,
                        )
                    previousInput = input
                    previousItems = items
                    items
                },
        )
    }

    private suspend fun buildItems(
        profileId: Long,
        favorites: List<Entry>,
        groups: List<LibraryGroupIdentity>,
        hiddenSources: Set<Long>,
    ): List<LibraryItem> {
        if (favorites.isEmpty()) return emptyList()

        val entryIds = favorites.map { it.id }
        val chapters = entryChapterRepository.getChaptersByEntryIds(entryIds).first()
        val categoryIdsByEntryId = categoryRepository.getCategoryIdsByEntryIds(profileId, entryIds)
        val lastReadByEntryId = entryRepository.getLibraryLastRead(profileId)

        return buildItems(
            favorites = favorites,
            groups = groups,
            hiddenSources = hiddenSources,
            chapters = chapters,
            categoryIdsByEntryId = categoryIdsByEntryId,
            lastReadByEntryId = lastReadByEntryId,
        )
    }

    private fun observeCategoryIds(
        profileId: Long,
        entryIds: List<Long>,
    ): Flow<Map<Long, List<Long>>> {
        return (categoryRepository as? LibraryCategoryMappingObserver)
            ?.observeCategoryIdsByEntryIds(profileId, entryIds)
            ?: flow { emit(categoryRepository.getCategoryIdsByEntryIds(profileId, entryIds)) }
    }

    private fun observeLibraryLastRead(profileId: Long): Flow<Map<Long, Long>> {
        return (entryRepository as? LibraryLastReadObserver)
            ?.observeLibraryLastRead(profileId)
            ?: flow { emit(entryRepository.getLibraryLastRead(profileId)) }
    }

    private suspend fun buildItems(
        favorites: List<Entry>,
        groups: List<LibraryGroupIdentity>,
        hiddenSources: Set<Long>,
        chapters: List<EntryChapter>,
        categoryIdsByEntryId: Map<Long, List<Long>>,
        lastReadByEntryId: Map<Long, Long>,
    ): List<LibraryItem> {
        if (favorites.isEmpty()) return emptyList()

        val chaptersByEntryId = chapters.groupBy(EntryChapter::entryId)
        val progressMembers = favorites.map { entry ->
            EntryLibraryProgressMember(
                entry = entry,
                chapters = chaptersByEntryId[entry.id].orEmpty(),
                lastRead = lastReadByEntryId[entry.id] ?: 0L,
            )
        }
        val libraryStateByEntryId = entryLibraryProgressResolver.calculateBatch(progressMembers)
        val itemsById = favorites.associate { entry ->
            val entryChapters = chaptersByEntryId[entry.id].orEmpty()
            val libraryState = libraryStateByEntryId[entry.id] ?: entryLibraryProgressResolver.calculate(
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
            val members = group.orderedEntryIds.mapNotNull(itemsById::get)
            when {
                members.size > 1 -> mergeEntryItem(group.visibleEntryId, members)
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
        private val LIBRARY_INVALIDATION_DEBOUNCE = 50.milliseconds
        private const val MULTI_SOURCE_ID = Long.MIN_VALUE
    }
}

private data class LibraryBuildInput(
    val favorites: List<Entry>,
    val groups: List<LibraryGroupIdentity>,
    val hiddenSources: Set<Long>,
    val chapters: List<EntryChapter>,
    val categoryIdsByEntryId: Map<Long, List<Long>>,
    val lastReadByEntryId: Map<Long, Long>,
)

private data class LibraryDetailInput(
    val chapters: List<EntryChapter>,
    val categoryIdsByEntryId: Map<Long, List<Long>>,
    val lastReadByEntryId: Map<Long, Long>,
)

private data class LibraryGroupIdentity(
    val visibleEntryId: Long,
    val orderedEntryIds: List<Long>,
)

private fun LibraryBuildInput.isPinOnlyUpdateFrom(previous: LibraryBuildInput): Boolean {
    return groups === previous.groups &&
        hiddenSources === previous.hiddenSources &&
        chapters === previous.chapters &&
        categoryIdsByEntryId === previous.categoryIdsByEntryId &&
        lastReadByEntryId === previous.lastReadByEntryId &&
        favorites.isPinOnlyUpdateFrom(previous.favorites)
}

private fun List<Entry>.isPinOnlyUpdateFrom(previous: List<Entry>): Boolean {
    if (size != previous.size) return false
    var pinChanged = false
    for (index in indices) {
        val currentEntry = this[index]
        val previousEntry = previous[index]
        pinChanged = pinChanged || currentEntry.libraryPinned != previousEntry.libraryPinned
        if (currentEntry.withoutPinMutationState() != previousEntry.withoutPinMutationState()) return false
    }
    return pinChanged
}

private fun Entry.withoutPinMutationState(): Entry {
    return copy(
        libraryPinned = false,
        lastModifiedAt = 0L,
        version = 0L,
    )
}

private fun List<LibraryItem>.applyEntryUpdates(favorites: List<Entry>): List<LibraryItem> {
    val favoritesById = favorites.associateBy(Entry::id)
    return map { item ->
        item.copy(
            entry = favoritesById.getValue(item.entry.id),
            memberEntries = item.memberEntries.map { member -> favoritesById.getValue(member.id) },
        )
    }
}

private fun EntryLibraryGroupResolution.toIdentity() = LibraryGroupIdentity(
    visibleEntryId = visibleEntry.id,
    orderedEntryIds = orderedEntries.map(Entry::id),
)
