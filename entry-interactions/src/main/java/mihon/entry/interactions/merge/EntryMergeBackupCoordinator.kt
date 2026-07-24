package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import tachiyomi.domain.entry.model.Entry

internal class EntryMergeBackupCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeBackupFeature {
    override suspend fun snapshotForBackup(subject: EntryMergeSubject): EntryMergeBackupMember? {
        val profile = host.profile(subject.profileId)
        val membership = profile.membership(subject.entryId) ?: return null
        val target = profile.entries(listOf(membership.targetEntryId)).singleOrNull() ?: return null
        val position = membership.orderedEntryIds.indexOf(subject.entryId).takeIf { it >= 0 } ?: return null
        return EntryMergeBackupMember(target.backupIdentity(), position)
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
                skipped += EntryMergeBackupSkippedGroup(group.target, invalidReason)
                return@forEach
            }

            val resolved = normalized.members.mapNotNull { member ->
                profile.resolveEntryIdentity(member.identity.asEntry(destinationProfileId))?.let { member to it }
            }
            val target = resolved.singleOrNull { it.first.identity == normalized.target }?.second
            if (target == null) {
                skipped += EntryMergeBackupSkippedGroup(group.target, EntryMergeBackupSkipReason.MISSING_TARGET)
                return@forEach
            }
            val orderedEntries = resolved.sortedBy { it.first.position }.map { it.second }.distinctBy(Entry::id)
            if (orderedEntries.size < 2) {
                skipped += EntryMergeBackupSkippedGroup(group.target, EntryMergeBackupSkipReason.INSUFFICIENT_MEMBERS)
                return@forEach
            }
            val expectedGroups = orderedEntries.mapNotNull { profile.membership(it.id) }
                .distinctBy(EntryMergeMembershipSnapshot::targetEntryId)
            when (
                profile.applyTransition(
                    EntryMergeHostTransition.RestoreBackupGroup(
                        operationId = newEntryMergeSessionId(),
                        profileId = destinationProfileId,
                        expectedGroups = expectedGroups,
                        targetEntryId = target.id,
                        orderedEntryIds = orderedEntries.map(Entry::id),
                    ),
                )
            ) {
                is EntryMergeHostTransitionResult.Applied -> applied++
                EntryMergeHostTransitionResult.Conflict ->
                    skipped +=
                        EntryMergeBackupSkippedGroup(group.target, EntryMergeBackupSkipReason.CONFLICT)
                is EntryMergeHostTransitionResult.OperationalFailure ->
                    skipped +=
                        EntryMergeBackupSkippedGroup(group.target, EntryMergeBackupSkipReason.OPERATIONAL_FAILURE)
            }
        }

        return EntryMergeBackupRestoreResult(appliedGroupCount = applied, skippedGroups = skipped)
    }
}

private data class NormalizedBackupGroup(
    val target: EntryMergeBackupIdentity,
    val members: List<EntryMergeBackupGroupMember>,
    val invalidReason: EntryMergeBackupSkipReason?,
)

private fun normalize(group: EntryMergeBackupGroup): NormalizedBackupGroup {
    val members = (group.members + EntryMergeBackupGroupMember(group.target, Int.MIN_VALUE))
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

private fun Entry.backupIdentity() = EntryMergeBackupIdentity(source, url, type)

private fun EntryMergeBackupIdentity.asEntry(profileId: Long): Entry {
    return Entry.create().copy(
        profileId = profileId,
        source = sourceId,
        url = url,
        type = type,
    )
}
