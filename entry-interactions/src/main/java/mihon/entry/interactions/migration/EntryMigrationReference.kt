package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry
import java.util.UUID

internal data class FeatureEntryMigrationReference(
    val sessionId: String,
    val source: Entry,
    val target: Entry,
    val availableOptions: Set<EntryMigrationOption>,
) : EntryMigrationReference

internal fun newEntryMigrationSessionId(): String = UUID.randomUUID().toString()

internal fun Entry.sameMigrationIdentity(other: Entry): Boolean {
    return id == other.id &&
        profileId == other.profileId &&
        type == other.type &&
        source == other.source &&
        url == other.url
}

internal fun Entry.sameMigrationAuthorization(other: Entry): Boolean {
    return sameMigrationIdentity(other) &&
        favorite == other.favorite &&
        dateAdded == other.dateAdded
}
