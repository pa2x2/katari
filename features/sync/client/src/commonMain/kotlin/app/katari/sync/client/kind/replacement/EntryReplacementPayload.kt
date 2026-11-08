package app.katari.sync.client.kind.replacement

import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.payload.UnknownFields
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
    /**
     * How a migration carried chapters, beyond what [replacement] says, which can't take new fields: an older app
     * rewriting the record keeps only the top-level fields it doesn't know ([UnknownFields]). Stamped as the
     * replacement it belongs to; such a rewrite can keep this from an earlier replacement, so it counts only while the
     * stamps match ([migratedChapters]).
     */
    @ProtoNumber(3) val chapters: Stamped<MigratedChapters>? = null,
) {
    val migratedChapters: MigratedChapters?
        get() = chapters?.takeIf { it.at == replacement.at }?.value
}

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

/** How a migration carried the old entry's chapters over, where [Replacement] can't say. */
@Serializable
data class MigratedChapters(
    /**
     * The old entry's chapters in the order the migration paired them by position, each with the new entry's chapter
     * it took, which no device could work out again from its own chapter lists; empty when it paired them by number.
     */
    @ProtoNumber(1) val inOrder: List<PairedChapter> = emptyList(),
    /**
     * Whether the migration took only the old entry's chapter state: a season folding into the entry its merged entry
     * migrated to, which leaves the new entry's library state, tracking and choices to the merged entry.
     */
    @ProtoNumber(2) val chapterStateOnly: Boolean = false,
)

@Serializable
data class PairedChapter(
    @ProtoNumber(1) val sourceUrl: String,
    /** Null where the new entry had no chapter left for it. */
    @ProtoNumber(2) val targetUrl: String? = null,
)
