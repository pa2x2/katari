package app.katari.sync.client.record

/** What a kind found for a local row. */
sealed interface LocalRead<out P : Any> {
    /**
     * The row exists and syncs as [key]. [parents] are the records its payload refers to: one never synced is synced
     * along with it, so another device doesn't hold this record waiting for a parent that never comes. [build] turns
     * the row into the payload, given the payload as last stored and the record's edits. [replaces] is the live record
     * the row was synced under before, by a key it no longer has, which stands for no row once this one takes over.
     */
    class Found<P : Any>(
        val key: String,
        val parents: List<RecordRef> = emptyList(),
        val replaces: String? = null,
        val build: (previous: P?, stamps: FieldStamps) -> P,
    ) : LocalRead<P>

    /** No such row, or none any more. */
    data object Missing : LocalRead<Nothing>

    /** The row exists but never syncs, such as a local-source entry or anything of one. */
    data object NotSynced : LocalRead<Nothing>
}

class RecordRef(val kind: SyncedKind<*>, val ref: LocalRef)
