package eu.kanade.tachiyomi.ui.entry.members

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.entry.components.mergedSourceNames
import eu.kanade.tachiyomi.source.getDisplayNameForEntryInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.merge.EntryMergeEditorProjection
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.merge.EntryMergeMetadataRefreshFeature
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.entry.interactions.merge.EntryMergePreparationResult
import mihon.entry.interactions.merge.EntryMergePrepareIntent
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.feature.library.search.librarySearchQuery
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.SourceManager

/**
 * The entries an entry screen covers: the entry alone, or every member of the merge it belongs to, unless the screen
 * opened one member on its own with [bypassMerge].
 */
@AssistedInject
class EntryMembers(
    @Assisted private val entryId: Long,
    @Assisted private val bypassMerge: Boolean,
    private val getEntry: GetEntry,
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryMergeFeature: EntryMergeFeature,
    private val entryMergeNavigationFeature: EntryMergeNavigationFeature,
    private val entryMergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature,
    private val sourceManager: SourceManager,
    private val sourcePreferences: SourcePreferences,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(entryId: Long, bypassMerge: Boolean): EntryMembers
    }

    fun entryAndChapters(): Flow<Pair<Entry, List<EntryChapter>>> {
        return getEntry.subscribe(entryId).flatMapLatest { entry ->
            getEntryWithChapters.subscribe(entry, bypassMerge = bypassMerge)
        }
    }

    /** The entry with the ids of the members it shows, again whenever the merge changes. */
    fun displayedIdChanges(): Flow<Pair<Entry, List<Long>>> {
        return getEntry.subscribe(entryId).flatMapLatest { entry ->
            entryMergeFeature.observeExisting(entry).map { editor ->
                val memberIds = if (bypassMerge) {
                    listOf(entry.id)
                } else {
                    editor?.entries?.map { it.entry.id }.orEmpty().ifEmpty { listOf(entry.id) }
                }
                entry to memberIds
            }
        }
    }

    suspend fun displayedIds(): List<Long> {
        if (bypassMerge) return listOf(entryId)
        val entry = entryOrNull(entryId) ?: return listOf(entryId)
        return existingMergeEditor(entry)?.entries?.map { it.entry.id }.orEmpty().ifEmpty { listOf(entryId) }
    }

    suspend fun displayedChapters(): List<EntryChapter> {
        val entry = entryOrNull(entryId) ?: return emptyList()
        return getEntryWithChapters.awaitChapters(entry, bypassMerge = bypassMerge)
    }

    suspend fun forEachDisplayed(block: suspend (Entry) -> Unit) {
        displayedIds().forEach { memberId ->
            entryOrNull(memberId)?.let { memberEntry -> block(memberEntry) }
        }
    }

    suspend fun presentation(entry: Entry): EntryMembersPresentation {
        val editor = existingMergeEditor(entry)
        val mergeGroupMemberIds = editor?.entries?.map { it.entry.id }.orEmpty()
            .ifEmpty { listOf(entry.id) }
            .toImmutableList()
        val memberIds = if (bypassMerge) {
            persistentListOf(entry.id)
        } else {
            mergeGroupMemberIds
        }
        val membersById = editor?.entries.orEmpty().associateBy { it.entry.id }
        val members = memberIds.mapNotNull { memberId -> membersById[memberId]?.entry ?: entryOrNull(memberId) }
        val sources = members.ifEmpty { listOf(entry) }
            .map(Entry::source)
            .distinct()
            .map(sourceManager::getDisplayInfo)
        val sourceNameById = sources.associate { it.id to it.getDisplayNameForEntryInfo(sourcePreferences) }
        val sourceName = mergedSourceNames(sourceNameById.values.toList())
        return EntryMembersPresentation(
            sourceName = sourceName,
            sourceSearchQuery = sources.singleOrNull()?.librarySearchQuery() ?: sourceName,
            isMultiSource = sources.size > 1,
            isSourceMissing = sources.any(SourceDisplayInfo::isMissing),
            memberIds = memberIds,
            memberTitleById = members.associate { it.id to it.displayTitle },
            memberSourceNameById = if (sources.size > 1) {
                members.associate { it.id to sourceNameById.getValue(it.source) }
            } else {
                emptyMap()
            },
            mergeTargetId = editor?.entries?.firstOrNull { it.reference == editor.target }?.entry?.id
                ?: entryMergeNavigationFeature.resolveNavigation(
                    EntryMergeSubject(entry.profileId, entry.id),
                ).visibleEntryId,
            mergeGroupMemberIds = mergeGroupMemberIds,
        )
    }

    suspend fun existingMergeEditor(entry: Entry): EntryMergeEditorProjection? {
        return when (val result = entryMergeFeature.prepare(EntryMergePrepareIntent(listOf(entry)))) {
            is EntryMergePreparationResult.Ready -> result.editor
            is EntryMergePreparationResult.Rejected -> null
        }
    }

    /** The members a merged entry still has to fetch from their sources; [current] is the entry as last shown. */
    suspend fun needingRefresh(current: Entry?): List<Entry> {
        val members = refreshOwners(current)
        if (members.size <= 1) return members

        return members.filter { memberEntry ->
            // A merged entry from a source holds no chapters; its members are in the merge already
            !memberEntry.initialized || (
                !memberEntry.mergedBySource &&
                    entryChapterRepository.getChaptersByEntryIdAwait(memberEntry.id, applyScanlatorFilter = false)
                        .isEmpty()
                )
        }
    }

    suspend fun toRefreshFromSource(manualFetch: Boolean, current: Entry?): List<Entry> {
        val mergedMembers = refreshOwners(current)
        if (mergedMembers.size <= 1) {
            return listOfNotNull(current ?: entryOrNull(entryId))
        }

        return if (manualFetch) {
            mergedMembers
        } else {
            needingRefresh(current)
        }
    }

    private suspend fun refreshOwners(current: Entry?): List<Entry> {
        val entry = current ?: entryOrNull(entryId) ?: return emptyList()
        return if (bypassMerge) {
            listOf(entry)
        } else {
            entryMergeMetadataRefreshFeature.resolveOwners(entry).orderedOwners
        }
    }

    private suspend fun entryOrNull(id: Long): Entry? {
        return runCatching { entryRepository.getEntryById(id) }.getOrNull()
    }
}

data class EntryMembersPresentation(
    val sourceName: String,
    val sourceSearchQuery: String,
    val isMultiSource: Boolean,
    /** Whether any member's source is missing, not just the visible entry's. */
    val isSourceMissing: Boolean,
    val memberIds: ImmutableList<Long>,
    val memberTitleById: Map<Long, String>,
    val memberSourceNameById: Map<Long, String>,
    val mergeTargetId: Long,
    val mergeGroupMemberIds: ImmutableList<Long>,
)
