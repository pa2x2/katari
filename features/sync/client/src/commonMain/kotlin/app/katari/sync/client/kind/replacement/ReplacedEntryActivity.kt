package app.katari.sync.client.kind.replacement

import app.katari.sync.client.apply.LocalWrites

/**
 * The entry that activity recorded on [entryKey] at [atMillis] (epoch milliseconds) belongs to. A replacement, by a
 * move to another profile or by a migration, takes the old entry's sessions and completions along
 * ([EntryReplacementKind]), so activity recorded on it by a device that hadn't heard of the replacement lands on the
 * new entry, as replacing it here would have left it. Once the person brought the old entry back, a moved entry is
 * where its activity belongs again; a migrated one keeps only what was recorded after its return, as the migration
 * took everything before.
 */
internal suspend fun LocalWrites.entryFollowingReplacement(entryKey: String, atMillis: Long): String {
    val standing = database.standingReplacement(entryKey) ?: return entryKey
    val returnedAt = standing.returnedAt
    val follows = when {
        returnedAt == null -> true
        standing.replacement.cause == Replacement.Cause.PROFILE_MOVE -> false
        else -> atMillis < returnedAt.physicalMillis
    }
    return if (follows) standing.replacement.to else entryKey
}
