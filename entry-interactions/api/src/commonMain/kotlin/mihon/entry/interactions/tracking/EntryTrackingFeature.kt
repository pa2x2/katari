package mihon.entry.interactions.tracking

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

/** Application-facing boundary for tracker-backed Entry behavior. */
interface EntryTrackingFeature :
    EntryTrackingOperations,
    EntryTrackingAutomation,
    EntryTrackingAccounts,
    EntryTrackingCollection {
    fun availability(entryType: EntryType): EntryTrackingAvailability

    /** Screens are Java-serialized into saved state, which the descriptor can't be, so they keep the id instead. */
    fun service(serviceId: EntryTrackingServiceId): EntryTrackingServiceDescriptor?

    fun observeSession(entry: Entry): Flow<EntryTrackingSession>
}
