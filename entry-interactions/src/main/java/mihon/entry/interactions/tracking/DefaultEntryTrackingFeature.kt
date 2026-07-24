package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntrySnapshot
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryTrackingFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val host: EntryTrackingHost,
    private val automation: DefaultEntryTrackingAutomation = DefaultEntryTrackingAutomation(evaluation, host),
) : EntryTrackingFeature,
    EntryTrackingOperations by DefaultEntryTrackingOperations(evaluation, host, automation),
    EntryTrackingAutomation by automation,
    EntryTrackingAccounts by DefaultEntryTrackingAccounts(host),
    EntryTrackingCollection by DefaultEntryTrackingCollection(host) {

    override fun availability(entryType: EntryType): EntryTrackingAvailability {
        val services = host.registeredServices().filter { entryType in it.supportedEntryTypes }
        evaluation.requireTrackingAvailability(entryType, services.isNotEmpty())
        return if (services.isEmpty()) {
            EntryTrackingAvailability.Unsupported
        } else {
            EntryTrackingAvailability.Available(services.map { it.toDescriptor() })
        }
    }

    override fun observeSession(entry: Entry): Flow<EntryTrackingSession> {
        return host.observeEntry(entry)
            .map { snapshot -> snapshot.toSession(entry.type) }
            .distinctUntilChanged()
    }

    private fun EntryTrackingHostEntrySnapshot.toSession(entryType: EntryType): EntryTrackingSession {
        val supported = services.filter { entryType in it.service.supportedEntryTypes }
        val authenticated = supported.filter { it.isLoggedIn }
        val accepted = authenticated.filter { it.acceptsSource }
        evaluation.requireTrackingSession(
            entryType = entryType,
            registered = supported.isNotEmpty(),
            authenticated = authenticated.isNotEmpty(),
            sourceAccepted = accepted.isNotEmpty(),
        )
        return when {
            supported.isEmpty() -> EntryTrackingSession.Unavailable(
                setOf(EntryTrackingSessionUnavailableReason.UNSUPPORTED_ENTRY_TYPE),
            )
            authenticated.isEmpty() -> EntryTrackingSession.Unavailable(
                setOf(EntryTrackingSessionUnavailableReason.NOT_LOGGED_IN),
            )
            accepted.isEmpty() -> EntryTrackingSession.Unavailable(
                setOf(EntryTrackingSessionUnavailableReason.SOURCE_NOT_ACCEPTED),
            )
            else -> EntryTrackingSession.Available(
                accepted.map { service ->
                    EntryTrackingSessionService(
                        service = service.service.toDescriptor(),
                        track = service.track?.toTrackingRecord(),
                        displayScore = service.displayScore,
                    )
                },
            )
        }
    }
}
