package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryGroupResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort

internal class EntryMergeLibraryGroupingCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeLibraryGroupingFeature, EntryLibraryGroupingResolutionPort {
    override suspend fun groupLibraryEntries(
        profileId: Long,
        entries: List<Entry>,
    ): EntryMergeLibraryGroupingProjection {
        return project(profileId, entries, host.profile(profileId).memberships()).toFeatureProjection()
    }

    override fun observeLibraryGroups(
        profileId: Long,
        entries: Flow<List<Entry>>,
    ): Flow<EntryMergeLibraryGroupingProjection> {
        return combine(entries, host.profile(profileId).observeMemberships()) { currentEntries, memberships ->
            project(profileId, currentEntries, memberships).toFeatureProjection()
        }
    }

    override suspend fun resolveLibraryGrouping(
        profileId: Long,
        entries: List<Entry>,
    ): EntryLibraryGroupingResolution {
        return project(profileId, entries, host.profile(profileId).memberships())
    }

    override fun observeLibraryGrouping(
        profileId: Long,
        entries: Flow<List<Entry>>,
    ): Flow<EntryLibraryGroupingResolution> {
        return combine(entries, host.profile(profileId).observeMemberships()) { currentEntries, memberships ->
            project(profileId, currentEntries, memberships)
        }
    }

    private fun project(
        profileId: Long,
        entries: List<Entry>,
        memberships: List<EntryMergeMembershipSnapshot>,
    ): EntryLibraryGroupingResolution {
        require(entries.all { it.profileId == profileId }) { "Library grouping cannot cross profiles" }
        val entriesById = entries.associateBy(Entry::id)
        val membershipByEntryId = memberships
            .flatMap { membership -> membership.orderedEntryIds.map { it to membership } }
            .toMap()
        val consumed = mutableSetOf<Long>()
        val groups = entries.mapNotNull { entry ->
            if (!consumed.add(entry.id)) return@mapNotNull null
            val membership = membershipByEntryId[entry.id]
            if (membership == null) {
                EntryLibraryGroupResolution(entry, listOf(entry))
            } else {
                val ordered = membership.orderedEntryIds.mapNotNull(entriesById::get)
                consumed += ordered.map(Entry::id)
                val visible = entriesById[membership.targetEntryId] ?: ordered.firstOrNull() ?: entry
                EntryLibraryGroupResolution(visible, ordered.ifEmpty { listOf(entry) })
            }
        }
        return EntryLibraryGroupingResolution(profileId, groups)
    }

    private fun EntryLibraryGroupingResolution.toFeatureProjection(): EntryMergeLibraryGroupingProjection {
        return EntryMergeLibraryGroupingProjection(
            profileId = profileId,
            groups = groups.map { group -> EntryMergeLibraryGroup(group.visibleEntry, group.orderedEntries) },
        )
    }
}
