package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

/** Explicit profile-scoped identity retained across Migration navigation and execution. */
data class EntryMigrationSubject(
    val profileId: Long,
    val entryId: Long,
)

/** Requests authoritative preparation for one concrete source/target pair. */
data class EntryMigrationPrepareIntent(
    val source: Entry,
    val target: Entry,
)

/** Requests source refresh during Migration target discovery or explicit target selection. */
data class EntryMigrationTargetRefreshIntent(
    val source: Entry,
    val target: Entry,
    val fetchDetails: Boolean,
    val fetchChildren: Boolean,
)

/** User-selectable transfer options captured before execution. */
enum class EntryMigrationOption {
    CHILD_STATE,
    CATEGORIES,
    NOTES,
    CUSTOM_COVER,
    REMOVE_SOURCE_DOWNLOADS,
}

enum class EntryMigrationMode {
    COPY,
    REPLACE,
}

/**
 * Executes choices made against a Feature-issued preparation.
 *
 * The selected options are user intent, not a downstream consequence checklist. Entry Migration owns the pipeline and
 * automatically composes every applicable non-optional Feature consequence.
 */
data class EntryMigrationExecuteIntent(
    val reference: EntryMigrationReference,
    val mode: EntryMigrationMode,
    val selectedOptions: Set<EntryMigrationOption>,
)

/** Opaque proof of the authoritative state used to prepare one Migration pair. */
interface EntryMigrationReference
