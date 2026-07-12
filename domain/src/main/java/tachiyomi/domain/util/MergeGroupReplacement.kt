package tachiyomi.domain.util

data class MergeGroupMember(
    val targetId: Long,
    val memberId: Long,
    val position: Long,
)

sealed interface MergeGroupReplacement {
    val targetId: Long
    val targetIdsToRemoveReplacementFrom: Set<Long>

    data class Upsert(
        override val targetId: Long,
        val orderedMemberIds: List<Long>,
        override val targetIdsToRemoveReplacementFrom: Set<Long>,
    ) : MergeGroupReplacement

    data class Delete(
        override val targetId: Long,
        override val targetIdsToRemoveReplacementFrom: Set<Long>,
    ) : MergeGroupReplacement
}

fun replaceMergeGroupMember(
    currentId: Long,
    replacementId: Long,
    currentGroup: List<MergeGroupMember>,
    replacementGroup: List<MergeGroupMember>,
): MergeGroupReplacement? {
    if (currentGroup.isEmpty()) return null

    val orderedCurrentGroup = currentGroup.sortedBy(MergeGroupMember::position)
    val oldTargetId = orderedCurrentGroup.first().targetId
    val replacementTargetIds = replacementGroup
        .map(MergeGroupMember::targetId)
        .filterNot { it == oldTargetId }
        .toSet()
    val orderedMemberIds = orderedCurrentGroup
        .map { member -> if (member.memberId == currentId) replacementId else member.memberId }
        .distinct()
    val newTargetId = when {
        oldTargetId == currentId -> replacementId
        oldTargetId in orderedMemberIds -> oldTargetId
        else -> orderedMemberIds.firstOrNull()
    }

    return if (newTargetId != null && orderedMemberIds.size > 1) {
        MergeGroupReplacement.Upsert(
            targetId = newTargetId,
            orderedMemberIds = orderedMemberIds,
            targetIdsToRemoveReplacementFrom = replacementTargetIds,
        )
    } else {
        MergeGroupReplacement.Delete(
            targetId = oldTargetId,
            targetIdsToRemoveReplacementFrom = replacementTargetIds,
        )
    }
}
