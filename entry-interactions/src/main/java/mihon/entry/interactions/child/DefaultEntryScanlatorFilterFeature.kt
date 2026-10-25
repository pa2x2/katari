package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import mihon.entry.interactions.runtime.EntryScanlatorFilterDataSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryScanlatorFilterFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryScanlatorFilterInteraction,
    private val dataSource: EntryScanlatorFilterDataSource,
) : EntryScanlatorFilterFeature {
    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun state(scope: EntryScanlatorFilterScope): EntryScanlatorFilterStateResult {
        if (scope.entry.type !in applicableTypes) {
            return EntryScanlatorFilterStateResult.Inapplicable(scope.entry.type)
        }
        return EntryScanlatorFilterStateResult.Available(loadState(scope, profileId = null))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(scope: EntryScanlatorFilterScope): EntryScanlatorFilterObservationResult {
        if (scope.entry.type !in applicableTypes) {
            return EntryScanlatorFilterObservationResult.Inapplicable(scope.entry.type)
        }
        val memberIds = scope.normalizedMemberIds()
        val states: Flow<EntryScanlatorFilterState> = merge(
            dataSource.childrenChanged(memberIds),
            dataSource.excludedScanlatorsChanged(profileId = null, entryIds = memberIds),
        )
            .onStart { emit(Unit) }
            .mapLatest { loadState(scope, profileId = null) }
            .distinctUntilChanged()
        return EntryScanlatorFilterObservationResult.Available(states)
    }

    override fun filter(
        entry: Entry,
        chapters: List<EntryChapter>,
        excludedScanlators: Set<String>,
    ): EntryScanlatorFilterResult {
        if (entry.type !in applicableTypes) return EntryScanlatorFilterResult.Inapplicable(entry.type)
        val normalizedExcluded = normalizeScanlators(entry, excludedScanlators)
        return EntryScanlatorFilterResult.Available(
            chapters.filterNot { chapter ->
                interaction.scanlatorFor(entry, chapter) in normalizedExcluded
            },
        )
    }

    override suspend fun setExcludedScanlators(
        scope: EntryScanlatorFilterScope,
        excludedScanlators: Set<String>,
    ): EntryScanlatorFilterMutationResult {
        if (scope.entry.type !in applicableTypes) {
            return EntryScanlatorFilterMutationResult.Inapplicable(scope.entry.type)
        }
        val memberIds = scope.normalizedMemberIds()
        val normalized = normalizeScanlators(scope.entry, excludedScanlators)
        val currentByMember = dataSource.excludedScanlators(profileId = null, entryIds = memberIds)
            .mapValues { (_, scanlators) -> normalizeScanlators(scope.entry, scanlators) }
        if (memberIds.all { currentByMember[it].orEmpty() == normalized }) {
            return EntryScanlatorFilterMutationResult.NoChange
        }
        dataSource.setExcludedScanlators(profileId = null, entryIds = memberIds, excluded = normalized)
        return EntryScanlatorFilterMutationResult.Applied(memberIds.size)
    }

    override suspend fun snapshot(profileId: Long, entry: Entry): EntryScanlatorFilterSnapshotResult {
        if (entry.type !in applicableTypes) return EntryScanlatorFilterSnapshotResult.Inapplicable(entry.type)
        val excluded = dataSource.excludedScanlators(
            profileId = profileId,
            entryIds = listOf(entry.id),
        )[entry.id].orEmpty()
        return EntryScanlatorFilterSnapshotResult.Available(normalizeScanlators(entry, excluded))
    }

    override suspend fun restore(
        profileId: Long,
        entry: Entry,
        excludedScanlators: Set<String>,
    ): EntryScanlatorFilterRestoreResult {
        if (entry.type !in applicableTypes) return EntryScanlatorFilterRestoreResult.Inapplicable(entry.type)
        if (excludedScanlators.isEmpty()) return EntryScanlatorFilterRestoreResult.NoChange
        val memberIds = listOf(entry.id)
        val current = normalizeScanlators(
            entry,
            dataSource.excludedScanlators(profileId = profileId, entryIds = memberIds)[entry.id].orEmpty(),
        )
        val restored = current + normalizeScanlators(entry, excludedScanlators)
        if (restored == current) return EntryScanlatorFilterRestoreResult.NoChange
        dataSource.setExcludedScanlators(profileId = profileId, entryIds = memberIds, excluded = restored)
        return EntryScanlatorFilterRestoreResult.Restored(memberIds.size)
    }

    private suspend fun loadState(
        scope: EntryScanlatorFilterScope,
        profileId: Long?,
    ): EntryScanlatorFilterState {
        val memberIds = scope.normalizedMemberIds()
        val available = dataSource.children(memberIds)
            .mapNotNullTo(linkedSetOf()) { interaction.scanlatorFor(scope.entry, it) }
        val excluded = dataSource.excludedScanlators(profileId = profileId, entryIds = memberIds)
            .values
            .flatten()
            .let { normalizeScanlators(scope.entry, it) }
        return EntryScanlatorFilterState(availableScanlators = available, excludedScanlators = excluded)
    }

    private fun normalizeScanlators(entry: Entry, scanlators: Collection<String>): Set<String> {
        return scanlators.mapNotNullTo(linkedSetOf()) { interaction.normalizeScanlator(entry, it) }
    }

    private fun EntryScanlatorFilterScope.normalizedMemberIds(): List<Long> {
        return memberIds.distinct().ifEmpty { listOf(entry.id) }
    }
}
