package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryChildGroupFilterFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryChildGroupFilterInteraction,
    private val dataSource: EntryChildGroupFilterDataSource,
) : EntryChildGroupFilterFeature {
    private val applicableTypes = evaluation.childGroupFilterProviderTypes()

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun state(scope: EntryChildGroupFilterScope): EntryChildGroupFilterStateResult {
        if (scope.entry.type !in applicableTypes) {
            return EntryChildGroupFilterStateResult.Inapplicable(scope.entry.type)
        }
        return EntryChildGroupFilterStateResult.Available(loadState(scope, profileId = null))
    }

    override fun observe(scope: EntryChildGroupFilterScope): EntryChildGroupFilterObservationResult {
        if (scope.entry.type !in applicableTypes) {
            return EntryChildGroupFilterObservationResult.Inapplicable(scope.entry.type)
        }
        val memberIds = scope.normalizedMemberIds()
        val states: Flow<EntryChildGroupFilterState> = merge(
            dataSource.childrenChanged(memberIds),
            dataSource.excludedGroupsChanged(profileId = null, entryIds = memberIds),
        )
            .onStart { emit(Unit) }
            .mapLatest { loadState(scope, profileId = null) }
            .distinctUntilChanged()
        return EntryChildGroupFilterObservationResult.Available(states)
    }

    override fun filter(
        entry: Entry,
        chapters: List<EntryChapter>,
        excludedGroups: Set<String>,
    ): EntryChildGroupFilterResult {
        if (entry.type !in applicableTypes) return EntryChildGroupFilterResult.Inapplicable(entry.type)
        val normalizedExcluded = normalizeGroups(entry, excludedGroups)
        return EntryChildGroupFilterResult.Available(
            chapters.filterNot { chapter ->
                interaction.groupFor(entry, chapter) in normalizedExcluded
            },
        )
    }

    override suspend fun setExcludedGroups(
        scope: EntryChildGroupFilterScope,
        excludedGroups: Set<String>,
    ): EntryChildGroupFilterMutationResult {
        if (scope.entry.type !in applicableTypes) {
            return EntryChildGroupFilterMutationResult.Inapplicable(scope.entry.type)
        }
        val memberIds = scope.normalizedMemberIds()
        val normalized = normalizeGroups(scope.entry, excludedGroups)
        val currentByMember = dataSource.excludedGroups(profileId = null, entryIds = memberIds)
            .mapValues { (_, groups) -> normalizeGroups(scope.entry, groups) }
        if (memberIds.all { currentByMember[it].orEmpty() == normalized }) {
            return EntryChildGroupFilterMutationResult.NoChange
        }
        dataSource.setExcludedGroups(profileId = null, entryIds = memberIds, excluded = normalized)
        return EntryChildGroupFilterMutationResult.Applied(memberIds.size)
    }

    override suspend fun snapshot(profileId: Long, entry: Entry): EntryChildGroupFilterSnapshotResult {
        if (entry.type !in applicableTypes) return EntryChildGroupFilterSnapshotResult.Inapplicable(entry.type)
        val excluded = dataSource.excludedGroups(profileId = profileId, entryIds = listOf(entry.id))[entry.id].orEmpty()
        return EntryChildGroupFilterSnapshotResult.Available(normalizeGroups(entry, excluded))
    }

    override suspend fun restore(entry: Entry, excludedGroups: Set<String>): EntryChildGroupFilterRestoreResult {
        if (entry.type !in applicableTypes) return EntryChildGroupFilterRestoreResult.Inapplicable(entry.type)
        if (excludedGroups.isEmpty()) return EntryChildGroupFilterRestoreResult.NoChange
        val memberIds = listOf(entry.id)
        val current = normalizeGroups(
            entry,
            dataSource.excludedGroups(profileId = null, entryIds = memberIds)[entry.id].orEmpty(),
        )
        val restored = current + normalizeGroups(entry, excludedGroups)
        if (restored == current) return EntryChildGroupFilterRestoreResult.NoChange
        dataSource.setExcludedGroups(profileId = null, entryIds = memberIds, excluded = restored)
        return EntryChildGroupFilterRestoreResult.Restored(memberIds.size)
    }

    private suspend fun loadState(
        scope: EntryChildGroupFilterScope,
        profileId: Long?,
    ): EntryChildGroupFilterState {
        val memberIds = scope.normalizedMemberIds()
        val available = dataSource.children(memberIds)
            .mapNotNullTo(linkedSetOf()) { interaction.groupFor(scope.entry, it) }
        val excluded = dataSource.excludedGroups(profileId = profileId, entryIds = memberIds)
            .values
            .flatten()
            .let { normalizeGroups(scope.entry, it) }
        return EntryChildGroupFilterState(availableGroups = available, excludedGroups = excluded)
    }

    private fun normalizeGroups(entry: Entry, groups: Collection<String>): Set<String> {
        return groups.mapNotNullTo(linkedSetOf()) { interaction.normalizeGroup(entry, it) }
    }

    private fun EntryChildGroupFilterScope.normalizedMemberIds(): List<Long> {
        return memberIds.distinct().ifEmpty { listOf(entry.id) }
    }
}
