package app.katari.sync.client.kind.replacement

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * "Entry [from] was replaced by another", so other devices move the entry with what hangs off it instead of keeping
 * the old one or deleting and recreating it.
 */
@Serializable
data class EntryReplacementPayload(
    @ProtoNumber(1) val from: String,
    @ProtoNumber(2) val replacement: Stamped<Replacement>,
)

@Serializable
data class Replacement(
    @ProtoNumber(1) val to: String,
    @ProtoNumber(2) val cause: Cause,
    /**
     * For a migration, the options the person chose, by [tachiyomi.domain.entry.migration.EntryMigrationOption] name,
     * so other devices carry later edits over as the migration did; a name this version doesn't know is ignored.
     */
    @ProtoNumber(3) val options: List<String> = emptyList(),
    /**
     * For a migration, the target's library state just before it, so a migration a question later turns down leaves
     * the target as it found it; null when the device that migrated didn't record it.
     */
    @ProtoNumber(4) val targetBefore: LibraryState? = null,
) {
    @Serializable
    enum class Cause {
        /** A source migration that replaced the entry; one that copied it leaves the old entry in place. */
        @ProtoNumber(1)
        MIGRATION,

        /** Moved to another profile, which changes its key. */
        @ProtoNumber(2)
        PROFILE_MOVE,
    }
}

/** An entry's place in the library, as [tachiyomi.domain.entry.migration.MigrationLibraryFields] names it. */
@Serializable
data class LibraryState(
    @ProtoNumber(1) val favorite: Boolean,
    @ProtoNumber(2) val libraryPinned: Boolean,
    @ProtoNumber(3) val chapterFlags: Long,
    /** Epoch milliseconds. */
    @ProtoNumber(4) val dateAdded: Long,
    @ProtoNumber(5) val notes: String,
)
