package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import java.util.UUID

internal class EntryMergeMigrationCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeMigrationFeature {
    override suspend fun participateInReplacementTransaction(
        intent: EntryMergeMigrationReplacementIntent,
    ): EntryMergeMigrationReplacementResult {
        require(intent.current.profileId == intent.replacement.profileId) {
            "Migration replacement cannot cross profiles"
        }
        require(intent.current.type == intent.replacement.type) {
            "Migration replacement requires matching Entry types"
        }
        if (intent.current.id == intent.replacement.id) return EntryMergeMigrationReplacementResult.Applied

        val profile = host.profile(intent.current.profileId)
        val currentGroup = profile.membership(intent.current.id)
            ?: return EntryMergeMigrationReplacementResult.NoMembership
        val replacementGroup = profile.membership(intent.replacement.id)
        val expectedGroups = listOfNotNull(currentGroup, replacementGroup)
            .distinctBy(EntryMergeMembershipSnapshot::targetEntryId)
        val replacements = replacementGroups(
            currentEntryId = intent.current.id,
            replacementEntryId = intent.replacement.id,
            currentGroup = currentGroup,
            replacementGroup = replacementGroup,
        )
        val operationId = UUID.randomUUID().toString()
        return when (
            val result = profile.applyTransition(
                EntryMergeHostTransition.ReplaceForMigration(
                    operationId = operationId,
                    profileId = intent.current.profileId,
                    expectedGroups = expectedGroups,
                    currentEntryId = intent.current.id,
                    replacementEntryId = intent.replacement.id,
                    replacementGroups = replacements,
                ),
            )
        ) {
            is EntryMergeHostTransitionResult.Applied -> EntryMergeMigrationReplacementResult.Applied
            EntryMergeHostTransitionResult.Conflict -> EntryMergeMigrationReplacementResult.Conflict
            is EntryMergeHostTransitionResult.OperationalFailure -> {
                EntryMergeMigrationReplacementResult.OperationalFailure(result.retryable)
            }
        }
    }
}

internal fun replacementGroups(
    currentEntryId: Long,
    replacementEntryId: Long,
    currentGroup: EntryMergeMembershipSnapshot,
    replacementGroup: EntryMergeMembershipSnapshot?,
): List<EntryMergeMembershipSnapshot> {
    val replacedIds = currentGroup.orderedEntryIds
        .map { entryId -> if (entryId == currentEntryId) replacementEntryId else entryId }
        .distinct()
    val currentReplacement = replacedIds.toSnapshotOrNull(
        profileId = currentGroup.profileId,
        preferredTargetId = if (currentGroup.targetEntryId == currentEntryId) {
            replacementEntryId
        } else {
            currentGroup.targetEntryId
        },
    )
    val otherReplacement = replacementGroup
        ?.takeIf { it.targetEntryId != currentGroup.targetEntryId }
        ?.orderedEntryIds
        ?.filterNot { it == replacementEntryId }
        ?.toSnapshotOrNull(
            profileId = currentGroup.profileId,
            preferredTargetId = replacementGroup.targetEntryId,
        )
    return listOfNotNull(currentReplacement, otherReplacement)
}

private fun List<Long>.toSnapshotOrNull(
    profileId: Long,
    preferredTargetId: Long,
): EntryMergeMembershipSnapshot? {
    if (size < 2) return null
    return EntryMergeMembershipSnapshot(
        profileId = profileId,
        targetEntryId = preferredTargetId.takeIf { it in this } ?: first(),
        orderedEntryIds = this,
    )
}
