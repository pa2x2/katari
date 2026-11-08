package tachiyomi.domain.entry.migration

/** What a migration carries over at the person's choice, captured before it runs. */
enum class EntryMigrationOption {
    CHILD_STATE,
    CATEGORIES,
    NOTES,
    CUSTOM_COVER,
    REMOVE_SOURCE_DOWNLOADS,
}

/**
 * A migration's mode and chosen options as `entry_migration_operations.intent_fingerprint` holds them:
 * `MODE:OPTION,OPTION`, options by name and sorted. A replayed migration must match it exactly, and sync reads the
 * options back from it so other devices carry over what this one did.
 */
object EntryMigrationFingerprint {
    fun of(mode: String, options: Set<EntryMigrationOption>): String =
        mode + ":" + options.map(EntryMigrationOption::name).sorted().joinToString(",")

    /** The options of [fingerprint]; a name this version doesn't know is left out. */
    fun options(fingerprint: String): Set<EntryMigrationOption> = fingerprint.substringAfter(':', "")
        .split(',')
        .mapNotNullTo(HashSet()) { name -> EntryMigrationOption.entries.firstOrNull { it.name == name } }
}
