package mihon.entry.interactions.host.tracking

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

interface EntryTrackingAutomationHost {
    fun isAutomaticProgressSynchronizationEnabled(): Boolean

    suspend fun bindAutomatically(
        entry: Entry,
        serviceIds: Set<Long>,
    ): List<EntryTrackingHostBindingOutcome>

    suspend fun synchronizeProgress(
        entryId: Long,
        serviceIds: Set<Long>,
        progress: Double,
        scheduleRetry: Boolean,
    ): List<EntryTrackingHostServiceFailure>

    suspend fun reconcileRemoteProgress(
        entry: Entry,
        serviceId: Long,
        track: EntryTrack,
    )

    suspend fun prepareMigrationTracks(
        source: Entry,
        target: Entry,
        tracks: List<EntryTrack>,
    ): List<EntryTrack>
}

sealed interface EntryTrackingHostBindingOutcome {
    val serviceId: Long
    val serviceName: String

    data class Bound(
        override val serviceId: Long,
        override val serviceName: String,
        val track: EntryTrack,
    ) : EntryTrackingHostBindingOutcome

    data class NoMatch(
        override val serviceId: Long,
        override val serviceName: String,
    ) : EntryTrackingHostBindingOutcome

    data class Failed(
        override val serviceId: Long,
        override val serviceName: String,
        val cause: Throwable,
    ) : EntryTrackingHostBindingOutcome
}

data class EntryTrackingHostServiceFailure(
    val serviceId: Long,
    val serviceName: String,
    val cause: Throwable,
)
