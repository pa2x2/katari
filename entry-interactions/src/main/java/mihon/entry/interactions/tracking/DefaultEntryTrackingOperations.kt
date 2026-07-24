package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryTrackingOperations(
    evaluation: FeatureGraphEvaluation,
    private val host: EntryTrackingHost,
    private val automation: DefaultEntryTrackingAutomation,
) : EntryTrackingOperations {
    private val resolver = EntryTrackingOperationResolver(evaluation, host)

    override suspend fun refresh(entry: Entry): EntryTrackingRefreshResult {
        val available = resolver.resolveAvailableServices(entry)
        if (available is ResolvedTrackingServices.Unavailable) {
            return EntryTrackingRefreshResult.Unavailable(available.reason)
        }
        val services = (available as ResolvedTrackingServices.Available).services
        return operationCatching(
            block = {
                val result = host.operations.refresh(
                    entryId = entry.id,
                    serviceIds = services.mapTo(mutableSetOf()) { it.service.id },
                )
                result.refreshedTracks.forEach { track ->
                    automation.reconcileRemoteProgress(entry, track.trackerId, track)
                }
                EntryTrackingRefreshResult.Completed(
                    result.failures.map { failure ->
                        EntryTrackingRefreshFailure(
                            serviceId = EntryTrackingServiceId(failure.serviceId),
                            serviceName = failure.serviceName,
                            cause = failure.cause,
                        )
                    },
                )
            },
            failed = EntryTrackingRefreshResult::Failed,
        )
    }

    override suspend fun search(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        query: String,
    ): EntryTrackingSearchResult {
        val service = resolver.resolveService(entry, serviceId)
        if (service is ResolvedTrackingService.Unavailable) {
            return EntryTrackingSearchResult.Unavailable(service.reason)
        }
        return operationCatching(
            block = { EntryTrackingSearchResult.Found(host.operations.search(serviceId.value, query)) },
            failed = EntryTrackingSearchResult::Failed,
        )
    }

    override suspend fun register(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        candidate: EntryTrackingSearchCandidate,
        private: Boolean,
    ): EntryTrackingOperationResult {
        val service = resolver.resolveService(entry, serviceId)
        if (service is ResolvedTrackingService.Unavailable) {
            return EntryTrackingOperationResult.Unavailable(service.reason)
        }
        if (candidate.serviceId != serviceId) {
            return EntryTrackingOperationResult.Unavailable(
                EntryTrackingOperationUnavailableReason.SEARCH_CANDIDATE_SERVICE_MISMATCH,
            )
        }
        val resolved = service as ResolvedTrackingService.Available
        if (private && !resolved.value.service.capabilities.supportsPrivateTracking) {
            return EntryTrackingOperationResult.Unavailable(
                EntryTrackingOperationUnavailableReason.PRIVATE_TRACKING_UNSUPPORTED,
            )
        }
        return operationCatching(
            block = {
                host.operations.register(serviceId.value, candidate, entry.id, private)?.let { track ->
                    automation.reconcileRemoteProgress(entry, serviceId.value, track)
                }
                EntryTrackingOperationResult.Completed
            },
            failed = EntryTrackingOperationResult::Failed,
        )
    }

    override suspend fun registerAutomatically(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
    ): EntryTrackingAutomaticRegistrationResult {
        val service = resolver.resolveService(entry, serviceId)
        if (service is ResolvedTrackingService.Unavailable) {
            return EntryTrackingAutomaticRegistrationResult.Unavailable(service.reason)
        }
        val resolved = service as ResolvedTrackingService.Available
        if (!resolved.value.service.capabilities.supportsAutomaticBinding) {
            return EntryTrackingAutomaticRegistrationResult.Unavailable(
                EntryTrackingOperationUnavailableReason.AUTOMATIC_BINDING_UNSUPPORTED,
            )
        }
        return operationCatching(
            block = {
                val track = host.operations.registerAutomatically(serviceId.value, entry)
                if (track != null) {
                    automation.reconcileRemoteProgress(entry, serviceId.value, track)
                    EntryTrackingAutomaticRegistrationResult.Registered
                } else {
                    EntryTrackingAutomaticRegistrationResult.NoMatch
                }
            },
            failed = EntryTrackingAutomaticRegistrationResult::Failed,
        )
    }

    override suspend fun mutate(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        mutation: EntryTrackingMutation,
    ): EntryTrackingOperationResult {
        val service = resolver.resolveService(entry, serviceId)
        if (service is ResolvedTrackingService.Unavailable) {
            return EntryTrackingOperationResult.Unavailable(service.reason)
        }
        val resolved = service as ResolvedTrackingService.Available
        val track = resolved.value.track ?: return EntryTrackingOperationResult.Unavailable(
            EntryTrackingOperationUnavailableReason.TRACK_NOT_FOUND,
        )
        resolver.unavailableReason(mutation, resolved.value)?.let { reason ->
            return EntryTrackingOperationResult.Unavailable(reason)
        }
        return operationCatching(
            block = {
                host.operations.mutate(serviceId.value, track, mutation)
                EntryTrackingOperationResult.Completed
            },
            failed = EntryTrackingOperationResult::Failed,
        )
    }

    override suspend fun remove(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        removeRemote: Boolean,
    ): EntryTrackingRemovalResult {
        val service = resolver.resolveService(entry, serviceId)
        if (service is ResolvedTrackingService.Unavailable) {
            return EntryTrackingRemovalResult.Unavailable(service.reason)
        }
        val resolved = service as ResolvedTrackingService.Available
        val track = resolved.value.track ?: return EntryTrackingRemovalResult.Unavailable(
            EntryTrackingOperationUnavailableReason.TRACK_NOT_FOUND,
        )
        if (removeRemote && !resolved.value.service.capabilities.supportsRemoteDeletion) {
            return EntryTrackingRemovalResult.Unavailable(
                EntryTrackingOperationUnavailableReason.REMOTE_DELETION_UNSUPPORTED,
            )
        }

        var remoteFailure: Throwable? = null
        if (removeRemote) {
            try {
                host.operations.deleteRemote(serviceId.value, track)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                remoteFailure = error
            }
        }
        return operationCatching(
            block = {
                host.operations.unregister(entry.id, serviceId.value)
                EntryTrackingRemovalResult.Removed(remoteFailure)
            },
            failed = EntryTrackingRemovalResult::Failed,
        )
    }
}

private inline fun <T> operationCatching(
    block: () -> T,
    failed: (Throwable) -> T,
): T {
    return try {
        block()
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        failed(error)
    }
}
