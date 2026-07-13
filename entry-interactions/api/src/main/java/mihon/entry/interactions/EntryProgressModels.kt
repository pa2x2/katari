package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressLocator

interface EntryProgressInteraction {
    suspend fun snapshot(entry: Entry): EntryProgressSnapshot

    suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot)

    suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    )
}

data class EntryProgressSnapshot(
    val states: List<EntryProgressStateSnapshot> = emptyList(),
)

data class EntryProgressStateSnapshot(
    val contentKey: String = "",
    val resourceKey: String,
    val sourceChildKey: String? = null,
    val resourceRevision: String? = null,
    val locator: EntryProgressLocator,
    val completed: Boolean = false,
    val locatorUpdatedAt: Long = 0,
    val completionUpdatedAt: Long = 0,
) {
    init {
        require(resourceKey.isNotBlank()) { "resource key must not be blank" }
        require(sourceChildKey == null || sourceChildKey.isNotBlank()) { "source child key must not be blank" }
        require(locatorUpdatedAt >= 0) { "locator update time must not be negative" }
        require(completionUpdatedAt >= 0) { "completion update time must not be negative" }
    }
}

data class EntryProgressResourceMapping(
    val sourceContentKey: String = "",
    val sourceResourceKey: String,
    val targetContentKey: String = "",
    val targetResourceKey: String,
    val targetChapterId: Long? = null,
) {
    init {
        require(sourceResourceKey.isNotBlank()) { "source resource key must not be blank" }
        require(targetResourceKey.isNotBlank()) { "target resource key must not be blank" }
        require(targetChapterId == null || targetChapterId >= 0) { "target chapter id must not be negative" }
    }
}
