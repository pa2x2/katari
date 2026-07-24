package mihon.entry.interactions.host.tracking

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

/** Segregated application adapter used only by the root Tracking Feature. */
interface EntryTrackingHost {
    val operations: EntryTrackingOperationHost
    val automation: EntryTrackingAutomationHost
    val accounts: EntryTrackingAccountHost
    val collection: EntryTrackingCollectionHost
    val backup: EntryTrackingBackupHost

    fun registeredServices(): List<EntryTrackingHostService>

    fun observeEntry(entry: Entry): Flow<EntryTrackingHostEntrySnapshot>
}

data class EntryTrackingHostEntrySnapshot(
    val services: List<EntryTrackingHostEntryService>,
)

data class EntryTrackingHostEntryService(
    val service: EntryTrackingHostService,
    val isLoggedIn: Boolean,
    val acceptsSource: Boolean,
    val track: EntryTrack?,
    val displayScore: String?,
)

data class EntryTrackingHostService(
    val id: Long,
    val name: String,
    val logoResource: Int,
    val supportedEntryTypes: Set<EntryType>,
    val capabilities: EntryTrackingHostServiceCapabilities,
)

data class EntryTrackingHostServiceCapabilities(
    val statuses: List<EntryTrackingHostStatus>,
    val scores: List<String>,
    val supportsReadingDates: Boolean,
    val supportsPrivateTracking: Boolean,
    val supportsRemoteDeletion: Boolean,
    val supportsAutomaticBinding: Boolean,
)

data class EntryTrackingHostStatus(
    val value: Long,
    val label: StringResource?,
)
