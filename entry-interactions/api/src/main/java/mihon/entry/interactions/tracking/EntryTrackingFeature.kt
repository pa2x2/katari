package mihon.entry.interactions

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

    fun observeSession(entry: Entry): Flow<EntryTrackingSession>
}
