package app.katari.sync.host

import app.katari.sync.client.handoff.EntryHandOff
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import mihon.entry.interactions.media.handoff.EntryProgressOffer
import mihon.entry.interactions.media.handoff.EntryProgressOffers
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Progress other devices made, as sync takes it in: the arrival names the device, and the entry's newest saved
 * progress, which the arrival just wrote, says where it got to.
 */
@Inject
@ContributesBinding(AppScope::class)
class SyncProgressOffers(
    private val handOff: EntryHandOff,
    private val progress: EntryProgressRepository,
) : EntryProgressOffers {
    override fun offers(entryIds: Set<Long>): Flow<EntryProgressOffer> = handOff.arrivals
        .filter { it.entryId in entryIds }
        .mapNotNull { arrival ->
            progress.getByEntryId(arrival.entryId)
                .maxByOrNull { it.locatorUpdatedAt }
                ?.let { EntryProgressOffer(arrival.deviceName, it) }
        }
}
