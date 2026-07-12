package tachiyomi.domain.entry.model

data class EntryMerge(
    val targetId: Long,
    val entryId: Long,
    val position: Long,
) {
    val isTarget: Boolean get() = targetId == entryId
}
