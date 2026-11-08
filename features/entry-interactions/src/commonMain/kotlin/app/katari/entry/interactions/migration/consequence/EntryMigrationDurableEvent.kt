package app.katari.entry.interactions.migration.consequence

import app.katari.entry.interactions.migration.EntryMigrationChildMatch
import app.katari.entry.interactions.migration.EntryMigrationOption
import tachiyomi.domain.entry.model.Entry

internal data class EntryMigrationDurableEvent(
    val operationId: String,
    val source: Entry,
    val target: Entry,
    val selectedOptions: Set<EntryMigrationOption>,
    val childMatches: List<EntryMigrationChildMatch>,
    /** Whether only chapter state moves, so the steps that carry the source's choices to the target leave them. */
    val chapterStateOnly: Boolean,
)
