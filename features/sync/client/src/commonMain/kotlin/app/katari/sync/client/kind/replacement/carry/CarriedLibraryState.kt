package app.katari.sync.client.kind.replacement.carry

import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.category.CategoryMembershipKind
import app.katari.sync.client.kind.category.CategoryMembershipPayload
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.entry.EntryPayload
import tachiyomi.domain.entry.migration.EntryMigrationOption
import tachiyomi.domain.entry.migration.MigrationLibraryFields
import tachiyomi.domain.entry.migration.migratedLibraryFields

/**
 * An entry's place in the library carried over a replacement: the entry's own fields, and its category links when
 * the migration kept categories.
 */
internal class CarriedLibraryState(private val carry: ReplacementCarry) {

    /**
     * A move carries every field. A migration carries what [migratedLibraryFields] derives from the old entry: pinning,
     * chapter display and filters, the date added, and the notes when the person kept them; and the viewer flags, which
     * migration copies with the viewer settings. Being in the library is the migration's own doing, not the old
     * entry's, and the display name stays with the entry it was given to. An old entry out of the library, as another
     * migration of it leaves it, no longer has a date added or pin to give. The custom cover carries when the person
     * migrated it along.
     */
    suspend fun entry(from: EntryPayload) {
        val to = carry.stored(EntryKind, carry.to) ?: return
        val carried = if (carry.isMove) {
            to.copy(
                favorite = carry.pick(from.favorite, to.favorite),
                dateAdded = carry.pick(from.dateAdded, to.dateAdded),
                displayName = carry.pick(from.displayName, to.displayName),
                notes = carry.pick(from.notes, to.notes),
                libraryPinned = carry.pick(from.libraryPinned, to.libraryPinned),
                viewerFlags = carry.pick(from.viewerFlags, to.viewerFlags),
                chapterFlags = carry.pick(from.chapterFlags, to.chapterFlags),
                customCover = carry.pick(from.customCover, to.customCover),
            )
        } else {
            val fields = migratedLibraryFields(
                source = from.libraryFields(),
                target = to.libraryFields(),
                replace = true,
                carryNotes = carry.carries(EntryMigrationOption.NOTES),
                now = to.dateAdded.value,
            )
            val inLibrary = from.favorite.value
            to.copy(
                dateAdded = if (inLibrary) {
                    carry.ruled(
                        fields.dateAdded,
                        from.dateAdded,
                        to.dateAdded,
                    )
                } else {
                    to.dateAdded
                },
                notes = carry.ruled(fields.notes, from.notes, to.notes),
                libraryPinned = if (inLibrary) {
                    carry.ruled(fields.libraryPinned, from.libraryPinned, to.libraryPinned)
                } else {
                    to.libraryPinned
                },
                viewerFlags = carry.pick(from.viewerFlags, to.viewerFlags),
                chapterFlags = carry.ruled(fields.chapterFlags, from.chapterFlags, to.chapterFlags),
                customCover = if (carry.carries(EntryMigrationOption.CUSTOM_COVER)) {
                    carry.after(from.customCover) ?: to.customCover
                } else {
                    to.customCover
                },
            )
        }
        if (carried != to) carry.writes.change(EntryKind, carry.to, carried)
    }

    /** A migration that kept categories gave the new entry the old one's; a move leaves them behind. */
    suspend fun membership(from: CategoryMembershipPayload) {
        if (carry.isMove || !carry.carries(EntryMigrationOption.CATEGORIES)) return
        if (carry.after(from.removed) == null) return
        carry.writes.change(
            CategoryMembershipKind,
            RecordKeys.derive(CategoryMembershipKind.name, carry.to, from.categoryKey),
            from.copy(entryKey = carry.to),
        )
    }

    private fun EntryPayload.libraryFields() = MigrationLibraryFields(
        favorite = favorite.value,
        libraryPinned = libraryPinned.value,
        chapterFlags = chapterFlags.value,
        dateAdded = dateAdded.value,
        notes = notes.value,
    )
}
