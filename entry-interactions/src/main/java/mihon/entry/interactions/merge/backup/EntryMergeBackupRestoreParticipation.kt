package mihon.entry.interactions.merge.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.merge.ENTRY_MERGE_BACKUP_STATE_ID
import mihon.entry.interactions.merge.EntryMergeBackupFeature
import mihon.entry.interactions.merge.EntryMergeBackupGroup
import mihon.entry.interactions.merge.EntryMergeBackupGroupMember
import mihon.entry.interactions.merge.EntryMergeBackupIdentity
import mihon.entry.interactions.merge.EntryMergeBackupMember
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalizingEvent
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreIssue
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSessionStateKey
import kotlin.collections.orEmpty
import kotlin.collections.removeAll

internal class EntryMergeBackupRestoreParticipation(
    private val feature: EntryMergeBackupFeature,
) {
    fun enqueue(event: EntryBackupRestoreEvent, member: EntryMergeBackupMember) {
        val pending = event.session.state(SESSION_STATE_KEY, ::PendingRestoreState).groups
        val key = PendingKey(event.profileId, event.entry.type)
        val target = member.target
        val identity = EntryMergeBackupIdentity(
            event.entry.source,
            event.entry.url,
            event.entry.type,
        )
        val groups = pending.getOrPut(key) { linkedMapOf() }
        val group = groups.getOrPut(target) { PendingGroup(target, mutableListOf()) }
        group.members.removeAll { it.identity == identity }
        group.members += EntryMergeBackupGroupMember(
            identity,
            member.position,
        )
        if (target == identity) {
            group.members.removeAll { it.identity == target }
            group.members += EntryMergeBackupGroupMember(
                target,
                member.position,
            )
        }
    }

    suspend fun finalize(event: EntryBackupRestoreFinalizingEvent) {
        val pending = event.session.stateOrNull(SESSION_STATE_KEY) ?: return
        val key = PendingKey(event.profileId, event.type)
        val groups = pending.groups.remove(key)?.values.orEmpty()
        if (pending.groups.isEmpty()) event.session.remove(SESSION_STATE_KEY)
        if (groups.isEmpty()) return
        val result = feature.restore(
            destinationProfileId = event.profileId,
            groups = groups.map {
                EntryMergeBackupGroup(
                    it.target,
                    it.members.toList(),
                )
            },
        )
        result.skippedGroups.forEach { skipped ->
            event.issues.add(
                EntryBackupRestoreIssue(
                    participantId = ENTRY_MERGE_BACKUP_STATE_ID,
                    description = "Skipped merge ${skipped.target.sourceId}:${skipped.target.url}: ${skipped.reason}",
                ),
            )
        }
    }
}

private data class PendingKey(
    val profileId: Long,
    val type: EntryType,
)

private class PendingRestoreState(
    val groups: MutableMap<PendingKey, LinkedHashMap<EntryMergeBackupIdentity, PendingGroup>> = linkedMapOf(),
)

private data class PendingGroup(
    val target: EntryMergeBackupIdentity,
    val members: MutableList<EntryMergeBackupGroupMember>,
)

private val SESSION_STATE_KEY =
    EntryBackupRestoreSessionStateKey<PendingRestoreState>(
        "entry.merge.backup.restore",
    )
