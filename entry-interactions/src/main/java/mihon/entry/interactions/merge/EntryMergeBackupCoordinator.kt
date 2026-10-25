package mihon.entry.interactions.merge

import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import mihon.entry.interactions.merge.host.EntryMergeProfileHost
import mihon.entry.interactions.merge.source.sourceGroupOrder
import tachiyomi.domain.entry.model.Entry

internal class EntryMergeBackupCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeBackupFeature {
    override suspend fun companionsForBackup(profileId: Long, entries: List<Entry>): List<Entry> {
        val profile = host.profile(profileId)
        val includedIds = entries.mapTo(mutableSetOf(), Entry::id)
        val missingIds = profile.memberships()
            .filter { group -> group.orderedEntryIds.any(includedIds::contains) }
            .flatMap(EntryMergeMembershipSnapshot::orderedEntryIds)
            .filterNot(includedIds::contains)
            .distinct()
        return profile.entries(missingIds)
    }

    override suspend fun snapshotForBackup(subject: EntryMergeSubject): EntryMergeBackupMember? {
        val profile = host.profile(subject.profileId)
        val membership = profile.membership(subject.entryId) ?: return null
        val target = profile.entries(listOf(membership.targetEntryId)).singleOrNull() ?: return null
        val position = membership.orderedEntryIds.indexOf(subject.entryId).takeIf { it >= 0 } ?: return null
        return EntryMergeBackupMember(
            target.backupIdentity(),
            position,
        )
    }

    override suspend fun restore(
        destinationProfileId: Long,
        groups: List<EntryMergeBackupGroup>,
    ): EntryMergeBackupRestoreResult {
        val profile = host.profile(destinationProfileId)
        val skipped = mutableListOf<EntryMergeBackupSkippedGroup>()
        var applied = 0

        groups.forEach { group ->
            val normalized = normalize(group)
            val invalidReason = normalized.invalidReason
            if (invalidReason != null) {
                skipped += EntryMergeBackupSkippedGroup(
                    group.target,
                    invalidReason,
                )
                return@forEach
            }

            val resolved = normalized.members.mapNotNull { member ->
                profile.resolveEntryIdentity(member.identity.asEntry(destinationProfileId))?.let { member to it }
            }
            val target = resolved.singleOrNull { it.first.identity == normalized.target }?.second
            if (target == null) {
                skipped += EntryMergeBackupSkippedGroup(
                    group.target,
                    EntryMergeBackupSkipReason.MISSING_TARGET,
                )
                return@forEach
            }
            val backedUpIds = resolved.sortedBy { it.first.position }.map { it.second.id }.distinct()
            val expectedGroups = backedUpIds.mapNotNull { profile.membership(it) }
                .distinctBy(EntryMergeMembershipSnapshot::targetEntryId)
            // Restore adds to the merges this install has: members the backup lacks, such as ones an older backup
            // left out, stay after the backup's own.
            val orderedEntryIds = profile.keepLockedOrder(
                target,
                (backedUpIds + expectedGroups.flatMap(EntryMergeMembershipSnapshot::orderedEntryIds)).distinct(),
            )
            if (orderedEntryIds.size < 2) {
                skipped += EntryMergeBackupSkippedGroup(
                    group.target,
                    EntryMergeBackupSkipReason.INSUFFICIENT_MEMBERS,
                )
                return@forEach
            }
            when (
                profile.applyTransition(
                    EntryMergeHostTransition.RestoreBackupGroup(
                        operationId = newEntryMergeSessionId(),
                        profileId = destinationProfileId,
                        expectedGroups = expectedGroups,
                        targetEntryId = target.id,
                        orderedEntryIds = orderedEntryIds,
                    ),
                )
            ) {
                is EntryMergeHostTransitionResult.Applied -> applied++
                EntryMergeHostTransitionResult.Conflict ->
                    skipped +=
                        EntryMergeBackupSkippedGroup(
                            group.target,
                            EntryMergeBackupSkipReason.CONFLICT,
                        )
                is EntryMergeHostTransitionResult.OperationalFailure ->
                    skipped +=
                        EntryMergeBackupSkippedGroup(
                            group.target,
                            EntryMergeBackupSkipReason.OPERATIONAL_FAILURE,
                        )
            }
        }

        return EntryMergeBackupRestoreResult(
            appliedGroupCount = applied,
            skippedGroups = skipped,
        )
    }
}

/** A merged entry's root stays first and the members its source lists stay in source order, as a refresh keeps them. */
private suspend fun EntryMergeProfileHost.keepLockedOrder(target: Entry, orderedEntryIds: List<Long>): List<Long> {
    if (!target.mergedBySource) return orderedEntryIds
    val listing = sourceListing(target.id)
    return sourceGroupOrder(
        current = orderedEntryIds,
        rootId = target.id,
        listed = listing,
        claimed = listing.filterTo(mutableSetOf(), orderedEntryIds::contains),
        unlinked = emptySet(),
    )
}

private data class NormalizedBackupGroup(
    val target: EntryMergeBackupIdentity,
    val members: List<EntryMergeBackupGroupMember>,
    val invalidReason: EntryMergeBackupSkipReason?,
)

private fun normalize(group: EntryMergeBackupGroup): NormalizedBackupGroup {
    val members = (
        group.members + EntryMergeBackupGroupMember(
            group.target,
            Int.MIN_VALUE,
        )
        )
        .distinctBy(EntryMergeBackupGroupMember::identity)
    val types = members.map { it.identity.type }.distinct()
    return NormalizedBackupGroup(
        target = group.target,
        members = members,
        invalidReason = EntryMergeBackupSkipReason.MIXED_ENTRY_TYPES.takeIf {
            types.size != 1 || types.single() != group.target.type
        },
    )
}

internal fun Entry.backupIdentity() =
    EntryMergeBackupIdentity(source, url, type)

internal fun EntryMergeBackupIdentity.asEntry(profileId: Long): Entry {
    return Entry.create().copy(
        profileId = profileId,
        source = sourceId,
        url = url,
        type = type,
    )
}
