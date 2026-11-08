package app.katari.entry.interactions.media.handoff

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryProgressState

/**
 * Where another device got to in an entry, taken in while the entry is open in a reader or the player here (D053).
 * [progress] is already saved on this device; the reader offers to go there, never moves by itself.
 */
data class EntryProgressOffer(
    /** As the account names the device; null when the server couldn't be asked. */
    val deviceName: String?,
    val progress: EntryProgressState,
)

/**
 * The progress other devices make in the entries open in a reader, such as a merged entry's members; none on a device
 * that doesn't sync.
 */
fun interface EntryProgressOffers {
    fun offers(entryIds: Set<Long>): Flow<EntryProgressOffer>
}
