package tachiyomi.domain.entry.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EntryProgressLocator(
    val kind: String,
    val position: Long? = null,
    val extent: Long? = null,
    val progression: Double? = null,
    val totalProgression: Double? = null,
    val extensions: JsonObject = JsonObject(emptyMap()),
) {
    init {
        require(kind.isNotBlank()) { "locator kind must not be blank" }
        require(position == null || position >= 0) { "locator position must not be negative" }
        require(extent == null || extent > 0) { "locator extent must be positive" }
        require(progression.isValidProgression()) { "locator progression must be finite and between 0 and 1" }
        require(totalProgression.isValidProgression()) {
            "locator total progression must be finite and between 0 and 1"
        }
    }

    val isEmpty: Boolean
        get() = position == null &&
            extent == null &&
            progression == null &&
            totalProgression == null &&
            extensions.isEmpty()
}

@Serializable
data class EntryProgressState(
    val entryId: Long,
    val chapterId: Long? = null,
    val contentKey: String = "",
    val resourceKey: String,
    val resourceRevision: String? = null,
    val locator: EntryProgressLocator,
    val completed: Boolean = false,
    val locatorUpdatedAt: Long = 0,
    val completionUpdatedAt: Long = 0,
) {
    init {
        require(entryId >= 0) { "entry id must not be negative" }
        require(chapterId == null || chapterId >= 0) { "chapter id must not be negative" }
        require(resourceKey.isNotBlank()) { "resource key must not be blank" }
        require(locatorUpdatedAt >= 0) { "locator update time must not be negative" }
        require(completionUpdatedAt >= 0) { "completion update time must not be negative" }
    }

    val identity: EntryProgressIdentity
        get() = EntryProgressIdentity(entryId, contentKey, resourceKey)

    fun mergeWith(incoming: EntryProgressState): EntryProgressState {
        require(identity == incoming.identity) { "progress identities must match" }

        val incomingLocatorIsNewer = incoming.locatorUpdatedAt > locatorUpdatedAt
        val incomingCompletionIsNewer = incoming.completionUpdatedAt > completionUpdatedAt

        return copy(
            chapterId = incoming.chapterId ?: chapterId,
            resourceRevision = if (incomingLocatorIsNewer) incoming.resourceRevision else resourceRevision,
            locator = if (incomingLocatorIsNewer) incoming.locator else locator,
            completed = if (incomingCompletionIsNewer) incoming.completed else completed,
            locatorUpdatedAt = maxOf(locatorUpdatedAt, incoming.locatorUpdatedAt),
            completionUpdatedAt = maxOf(completionUpdatedAt, incoming.completionUpdatedAt),
        )
    }

    /**
     * [edit], made on this device just now, over this state. A time above 0 in [edit] marks a field it sets, which
     * takes the edit's value dated after this state's, so it replaces what is held. A time of 0 leaves the field as it
     * is. [mergeWith] would compare the times, but this state's can come from another device's clock: a position
     * synced from a device whose clock is ahead would win over reading done here after it arrived, and sync carries
     * these times, so the reading would be lost on every device.
     */
    fun withEdit(edit: EntryProgressState): EntryProgressState {
        require(identity == edit.identity) { "progress identities must match" }

        val setsLocator = edit.locatorUpdatedAt > 0
        val setsCompletion = edit.completionUpdatedAt > 0

        return copy(
            chapterId = edit.chapterId ?: chapterId,
            resourceRevision = if (setsLocator) edit.resourceRevision else resourceRevision,
            locator = if (setsLocator) edit.locator else locator,
            completed = if (setsCompletion) edit.completed else completed,
            locatorUpdatedAt = if (setsLocator) after(locatorUpdatedAt, edit.locatorUpdatedAt) else locatorUpdatedAt,
            completionUpdatedAt = if (setsCompletion) {
                after(completionUpdatedAt, edit.completionUpdatedAt)
            } else {
                completionUpdatedAt
            },
        )
    }
}

// Past the held time by at least 1, so the edit also sorts after a synced time that shares its millisecond
private fun after(held: Long, edited: Long): Long = maxOf(edited, held + 1)

@Serializable
data class EntryProgressIdentity(
    val entryId: Long,
    val contentKey: String,
    val resourceKey: String,
)

private fun Double?.isValidProgression(): Boolean {
    return this == null || (isFinite() && this in 0.0..1.0)
}
