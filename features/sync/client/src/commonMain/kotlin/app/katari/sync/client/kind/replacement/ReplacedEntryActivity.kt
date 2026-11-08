package app.katari.sync.client.kind.replacement

import app.katari.sync.client.apply.LocalWrites

/**
 * The entry that activity recorded on [entryKey] belongs to. A replacement, by a move to another profile or by a
 * migration, takes the old entry's sessions and completions along ([EntryReplacementKind]), so activity recorded on it
 * by a device that hadn't heard of the replacement lands on the new entry, as replacing it here would have left it.
 */
internal suspend fun LocalWrites.entryFollowingReplacement(entryKey: String): String =
    replacementOf(entryKey) ?: entryKey
