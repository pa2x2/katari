package tachiyomi.domain.entry.migration

/** The fields of an entry's library state that a migration sets on its target. */
data class MigrationLibraryFields(
    val favorite: Boolean,
    val libraryPinned: Boolean,
    val chapterFlags: Long,
    val dateAdded: Long,
    val notes: String,
)

/**
 * What [target] holds after [source] migrated to it: in the library, pinned if either was, with the source's chapter
 * display and filters, the source's date added when it replaces the source ([replace]) or [now] when it is a copy, and
 * the source's notes when [carryNotes].
 */
fun migratedLibraryFields(
    source: MigrationLibraryFields,
    target: MigrationLibraryFields,
    replace: Boolean,
    carryNotes: Boolean,
    now: Long,
): MigrationLibraryFields = MigrationLibraryFields(
    favorite = true,
    libraryPinned = source.libraryPinned || target.libraryPinned,
    chapterFlags = source.chapterFlags,
    dateAdded = if (replace) source.dateAdded else now,
    notes = if (carryNotes) source.notes else target.notes,
)
