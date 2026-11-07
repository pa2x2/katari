package app.katari.sync.client.apply

import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.LocalRef

/** What became of a record written to this device's rows. */
internal sealed interface ApplyOutcome {
    /**
     * Written. [ref] locates the row that holds the record now, or is null when no row does: a link to a deleted
     * category, a local-source entry, a replacement, which only redirects other records.
     */
    data class Applied(val ref: LocalRef?) : ApplyOutcome

    /** The record it refers to by [key] isn't on this device yet; it is written once that one is. */
    data class WaitsFor(val key: String) : ApplyOutcome

    /**
     * Not written for now for a reason that passes ([HoldReason]); tried again with the next sync, or after an app
     * update for [HoldReason.FAILED].
     */
    data class Held(val reason: HoldReason) : ApplyOutcome

    /**
     * The record gave way to another one that takes its row: it is deleted as [tombstone], a change this device
     * pushes. Two categories of one name in a profile end up as one this way.
     */
    data class Superseded(val tombstone: Tombstone) : ApplyOutcome
}

/** The local ID a record key stands for on this device, as a kind looks up the records its record refers to. */
internal sealed interface Resolved {
    data class Found(val id: Long) : Resolved

    /** No record of [key] here yet. */
    data class Missing(val key: String) : Resolved

    /** The record is deleted, so nothing that refers to it has a row to go to. */
    data object Gone : Resolved
}

/** The ID found, or else returns what the record waits for (missing) or that it applied to no row (gone). */
internal inline fun Resolved.idOr(otherwise: (ApplyOutcome) -> Nothing): Long = when (this) {
    is Resolved.Found -> id
    is Resolved.Missing -> otherwise(ApplyOutcome.WaitsFor(key))
    Resolved.Gone -> otherwise(ApplyOutcome.Applied(null))
}
