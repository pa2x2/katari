package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindingOutcome
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntryService
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

internal class DefaultEntryTrackingAutomation(
    private val evaluation: FeatureGraphEvaluation,
    private val host: EntryTrackingHost,
) : EntryTrackingAutomation {

    override suspend fun bindAutomatically(entry: Entry): EntryTrackingAutomaticBindingResult {
        val services = host.observeEntry(entry).first().services
        val supported = services.filter { entry.type in it.service.supportedEntryTypes }
        val authenticated = supported.filter { it.isLoggedIn }
        val automatic = authenticated.filter {
            it.acceptsSource && it.service.capabilities.supportsAutomaticBinding
        }
        evaluation.requireTrackingAutomaticBinding(
            entryType = entry.type,
            registered = supported.isNotEmpty(),
            authenticated = authenticated.isNotEmpty(),
            sourceAccepted = automatic.isNotEmpty(),
        )
        automaticUnavailable(supported, authenticated, automatic)?.let {
            return EntryTrackingAutomaticBindingResult.Unavailable(it)
        }

        val outcomes = host.automation.bindAutomatically(entry, automatic.mapTo(mutableSetOf()) { it.service.id })
        val bound = outcomes.filterIsInstance<EntryTrackingHostBindingOutcome.Bound>()
        bound.forEach { outcome -> reconcileRemoteProgress(entry, outcome.serviceId, outcome.track) }
        return EntryTrackingAutomaticBindingResult.Completed(
            boundServices = bound.mapTo(mutableSetOf()) { EntryTrackingServiceId(it.serviceId) },
            unmatchedServices = outcomes.filterIsInstance<EntryTrackingHostBindingOutcome.NoMatch>()
                .mapTo(mutableSetOf()) { EntryTrackingServiceId(it.serviceId) },
            failures = outcomes.filterIsInstance<EntryTrackingHostBindingOutcome.Failed>().map { it.toFailure() },
        )
    }

    override suspend fun inspectProgressSynchronization(
        entry: Entry,
        progress: Double,
    ): EntryTrackingProgressInspection {
        val resolved = resolveSynchronization(entry)
        if (resolved is SynchronizationServices.Unavailable) {
            return EntryTrackingProgressInspection.Unavailable(resolved.reason)
        }
        val services = (resolved as SynchronizationServices.Available).services
        return if (services.any { it.track!!.progress < progress }) {
            EntryTrackingProgressInspection.UpdateRequired
        } else {
            EntryTrackingProgressInspection.Current
        }
    }

    override suspend fun synchronizeProgress(
        entry: Entry,
        progress: Double,
        scheduleRetry: Boolean,
    ): EntryTrackingProgressSynchronizationResult {
        val resolved = resolveSynchronization(entry)
        if (resolved is SynchronizationServices.Unavailable) {
            return EntryTrackingProgressSynchronizationResult.Unavailable(resolved.reason)
        }
        val services = (resolved as SynchronizationServices.Available).services
        val failures = host.automation.synchronizeProgress(
            entryId = entry.id,
            serviceIds = services.mapTo(mutableSetOf()) { it.service.id },
            progress = progress,
            scheduleRetry = scheduleRetry,
        )
        return EntryTrackingProgressSynchronizationResult.Completed(
            failures.map { failure ->
                EntryTrackingServiceFailure(
                    EntryTrackingServiceId(failure.serviceId),
                    failure.serviceName,
                    failure.cause,
                )
            },
        )
    }

    override suspend fun prepareMigrationTracks(
        source: Entry,
        target: Entry,
        tracks: List<EntryTrackingRecord>,
    ): EntryTrackingMigrationPreparationResult {
        return try {
            EntryTrackingMigrationPreparationResult.Prepared(
                host.automation.prepareMigrationTracks(source, target, tracks.map(EntryTrackingRecord::toDomainTrack))
                    .map { it.toTrackingRecord() },
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            EntryTrackingMigrationPreparationResult.Failed(error)
        }
    }

    private suspend fun resolveSynchronization(entry: Entry): SynchronizationServices {
        val services = host.observeEntry(entry).first().services
        val supported = services.filter { entry.type in it.service.supportedEntryTypes }
        val authenticated = supported.filter { it.isLoggedIn }
        val tracked = authenticated.filter { it.track != null }
        evaluation.requireTrackingSynchronization(
            entryType = entry.type,
            registered = supported.isNotEmpty(),
            authenticated = authenticated.isNotEmpty(),
            existingTrack = tracked.isNotEmpty(),
        )
        return synchronizationUnavailable(supported, authenticated, tracked)?.let {
            SynchronizationServices.Unavailable(it)
        } ?: SynchronizationServices.Available(tracked)
    }

    internal suspend fun reconcileRemoteProgress(entry: Entry, serviceId: Long, track: EntryTrack) {
        evaluation.requireTrackingSynchronization(
            entryType = entry.type,
            registered = true,
            authenticated = true,
            existingTrack = true,
        )
        host.automation.reconcileRemoteProgress(entry, serviceId, track)
    }
}

private fun automaticUnavailable(
    supported: List<EntryTrackingHostEntryService>,
    authenticated: List<EntryTrackingHostEntryService>,
    automatic: List<EntryTrackingHostEntryService>,
): EntryTrackingAutomationUnavailableReason? = when {
    supported.isEmpty() -> EntryTrackingAutomationUnavailableReason.UNSUPPORTED_ENTRY_TYPE
    authenticated.isEmpty() -> EntryTrackingAutomationUnavailableReason.NOT_LOGGED_IN
    automatic.isEmpty() && authenticated.none { it.service.capabilities.supportsAutomaticBinding } ->
        EntryTrackingAutomationUnavailableReason.NO_AUTOMATIC_SERVICE
    automatic.isEmpty() -> EntryTrackingAutomationUnavailableReason.SOURCE_NOT_ACCEPTED
    else -> null
}

private fun synchronizationUnavailable(
    supported: List<EntryTrackingHostEntryService>,
    authenticated: List<EntryTrackingHostEntryService>,
    tracked: List<EntryTrackingHostEntryService>,
): EntryTrackingAutomationUnavailableReason? = when {
    supported.isEmpty() -> EntryTrackingAutomationUnavailableReason.UNSUPPORTED_ENTRY_TYPE
    authenticated.isEmpty() -> EntryTrackingAutomationUnavailableReason.NOT_LOGGED_IN
    tracked.isEmpty() -> EntryTrackingAutomationUnavailableReason.NO_TRACKED_SERVICE
    else -> null
}

private fun EntryTrackingHostBindingOutcome.Failed.toFailure() = EntryTrackingServiceFailure(
    serviceId = EntryTrackingServiceId(serviceId),
    serviceName = serviceName,
    cause = cause,
)

private sealed interface SynchronizationServices {
    data class Available(val services: List<EntryTrackingHostEntryService>) : SynchronizationServices

    data class Unavailable(val reason: EntryTrackingAutomationUnavailableReason) : SynchronizationServices
}
