package mihon.entry.interactions

import kotlinx.coroutines.flow.first
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntryService
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry

internal class EntryTrackingOperationResolver(
    private val evaluation: FeatureGraphEvaluation,
    private val host: EntryTrackingHost,
) {
    suspend fun resolveService(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
    ): ResolvedTrackingService {
        val services = host.observeEntry(entry).first().services
        evaluateSession(entry, services)
        val service = services.firstOrNull { it.service.id == serviceId.value }
            ?: return ResolvedTrackingService.Unavailable(
                EntryTrackingOperationUnavailableReason.SERVICE_NOT_REGISTERED,
            )
        return when {
            entry.type !in service.service.supportedEntryTypes -> ResolvedTrackingService.Unavailable(
                EntryTrackingOperationUnavailableReason.UNSUPPORTED_ENTRY_TYPE,
            )
            !service.isLoggedIn -> ResolvedTrackingService.Unavailable(
                EntryTrackingOperationUnavailableReason.NOT_LOGGED_IN,
            )
            !service.acceptsSource -> ResolvedTrackingService.Unavailable(
                EntryTrackingOperationUnavailableReason.SOURCE_NOT_ACCEPTED,
            )
            else -> ResolvedTrackingService.Available(service)
        }
    }

    suspend fun resolveAvailableServices(entry: Entry): ResolvedTrackingServices {
        val services = host.observeEntry(entry).first().services
        evaluateSession(entry, services)
        val supported = services.filter { entry.type in it.service.supportedEntryTypes }
        val authenticated = supported.filter { it.isLoggedIn }
        val accepted = authenticated.filter { it.acceptsSource }
        return when {
            supported.isEmpty() -> ResolvedTrackingServices.Unavailable(
                EntryTrackingOperationUnavailableReason.UNSUPPORTED_ENTRY_TYPE,
            )
            authenticated.isEmpty() -> ResolvedTrackingServices.Unavailable(
                EntryTrackingOperationUnavailableReason.NOT_LOGGED_IN,
            )
            accepted.isEmpty() -> ResolvedTrackingServices.Unavailable(
                EntryTrackingOperationUnavailableReason.SOURCE_NOT_ACCEPTED,
            )
            else -> ResolvedTrackingServices.Available(accepted)
        }
    }

    fun unavailableReason(
        mutation: EntryTrackingMutation,
        service: EntryTrackingHostEntryService,
    ): EntryTrackingOperationUnavailableReason? {
        val capabilities = service.service.capabilities
        return when (mutation) {
            is EntryTrackingMutation.Status ->
                EntryTrackingOperationUnavailableReason.INVALID_STATUS
                    .takeUnless { capabilities.statuses.any { it.value == mutation.value } }
            is EntryTrackingMutation.Score ->
                EntryTrackingOperationUnavailableReason.INVALID_SCORE
                    .takeUnless { mutation.value in capabilities.scores }
            is EntryTrackingMutation.StartDate,
            is EntryTrackingMutation.FinishDate,
            -> EntryTrackingOperationUnavailableReason.READING_DATES_UNSUPPORTED
                .takeUnless { capabilities.supportsReadingDates }
            is EntryTrackingMutation.Private ->
                EntryTrackingOperationUnavailableReason.PRIVATE_TRACKING_UNSUPPORTED
                    .takeUnless { capabilities.supportsPrivateTracking }
            is EntryTrackingMutation.Progress -> null
        }
    }

    private fun evaluateSession(entry: Entry, services: List<EntryTrackingHostEntryService>) {
        val supported = services.filter { entry.type in it.service.supportedEntryTypes }
        val authenticated = supported.filter { it.isLoggedIn }
        val accepted = authenticated.filter { it.acceptsSource }
        evaluation.requireTrackingSession(
            entryType = entry.type,
            registered = supported.isNotEmpty(),
            authenticated = authenticated.isNotEmpty(),
            sourceAccepted = accepted.isNotEmpty(),
        )
    }
}

internal sealed interface ResolvedTrackingService {
    data class Available(val value: EntryTrackingHostEntryService) : ResolvedTrackingService

    data class Unavailable(val reason: EntryTrackingOperationUnavailableReason) : ResolvedTrackingService
}

internal sealed interface ResolvedTrackingServices {
    data class Available(
        val services: List<EntryTrackingHostEntryService>,
    ) : ResolvedTrackingServices

    data class Unavailable(val reason: EntryTrackingOperationUnavailableReason) : ResolvedTrackingServices
}
