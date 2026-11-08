package app.katari.sync.client.kind.replacement.carry

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer

/**
 * What the app's own migration decides from things only the app knows, so later edits of a migrated entry carry over
 * on every device as the migration carried the entry: which chapter marks it moves for each entry type, and which
 * trackers' bindings it copies as they are rather than looking the new entry up on the tracker.
 */
interface MigrationCarryRules {
    fun chapterMarks(type: EntryType): MigrationChildStateTransfer

    fun copiesTrackerBinding(trackerId: Long): Boolean
}
