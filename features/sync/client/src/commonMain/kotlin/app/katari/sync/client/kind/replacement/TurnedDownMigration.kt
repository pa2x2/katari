package app.katari.sync.client.kind.replacement

import app.katari.sync.client.kind.entry.EntryPayload
import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp
import tachiyomi.domain.entry.migration.MigrationLibraryFields

/**
 * The entry a migration turned down by an answer had migrated to, left in the library as if that migration had never
 * been made: as it was before it, [before], when it was in the library then; otherwise out of it, as migrating away
 * from an entry leaves that ([MigrationLibraryFields.replaced]). What changes is stamped [at], the answer.
 */
internal fun EntryPayload.withoutMigration(before: LibraryState, at: HybridTimestamp): EntryPayload {
    val restored = if (before.favorite) before.fields() else libraryFields().replaced()
    fun <T> Stamped<T>.restoredTo(value: T) = if (value == this.value) this else Stamped(value, at)
    return copy(
        favorite = favorite.restoredTo(restored.favorite),
        libraryPinned = libraryPinned.restoredTo(restored.libraryPinned),
        chapterFlags = chapterFlags.restoredTo(restored.chapterFlags),
        dateAdded = dateAdded.restoredTo(restored.dateAdded),
        notes = notes.restoredTo(restored.notes),
    )
}

private fun EntryPayload.libraryFields() =
    MigrationLibraryFields(favorite.value, libraryPinned.value, chapterFlags.value, dateAdded.value, notes.value)

private fun LibraryState.fields() = MigrationLibraryFields(favorite, libraryPinned, chapterFlags, dateAdded, notes)
