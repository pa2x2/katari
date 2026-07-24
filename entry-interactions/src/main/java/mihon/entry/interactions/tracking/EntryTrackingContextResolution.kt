package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.host.tracking.EntryTrackingHostService
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.contextEvidence

internal fun FeatureGraphEvaluation.requireTrackingAvailability(
    entryType: EntryType,
    registered: Boolean,
) {
    requireEntryContextState(
        type = entryType,
        feature = ENTRY_TRACKING_FEATURE_ID,
        integration = EntryTrackingIntegration.AVAILABILITY.id,
        behaviorProjections = listOf(
            EntryTrackingBehavior.ENTRY_ACTION.id,
            EntryTrackingBehavior.DOCUMENTATION.id,
        ),
        evidence = listOf(contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, registered)),
        applicable = registered,
    )
}

internal fun FeatureGraphEvaluation.requireTrackingSession(
    entryType: EntryType,
    registered: Boolean,
    authenticated: Boolean,
    sourceAccepted: Boolean,
) {
    requireEntryContextState(
        type = entryType,
        feature = ENTRY_TRACKING_FEATURE_ID,
        integration = EntryTrackingIntegration.SESSION.id,
        behaviorProjections = listOf(
            EntryTrackingBehavior.ENTRY_SESSION.id,
            EntryTrackingBehavior.ENTRY_OPERATIONS.id,
        ),
        evidence = listOf(
            contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, registered),
            contextEvidence(ENTRY_TRACKING_AUTHENTICATED_SUPPORT, authenticated),
            contextEvidence(ENTRY_TRACKING_SOURCE_ACCEPTED, sourceAccepted),
        ),
        applicable = registered && authenticated && sourceAccepted,
    )
}

internal fun FeatureGraphEvaluation.requireTrackingAutomaticBinding(
    entryType: EntryType,
    registered: Boolean,
    authenticated: Boolean,
    sourceAccepted: Boolean,
) {
    requireEntryContextState(
        type = entryType,
        feature = ENTRY_TRACKING_FEATURE_ID,
        integration = EntryTrackingIntegration.AUTOMATIC_BINDING.id,
        behaviorProjections = listOf(EntryTrackingBehavior.AUTOMATIC_BINDING.id),
        evidence = listOf(
            contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, registered),
            contextEvidence(ENTRY_TRACKING_AUTHENTICATED_SUPPORT, authenticated),
            contextEvidence(ENTRY_TRACKING_AUTOMATIC_SOURCE_ACCEPTED, sourceAccepted),
        ),
        applicable = registered && authenticated && sourceAccepted,
    )
}

internal fun FeatureGraphEvaluation.requireTrackingSynchronization(
    entryType: EntryType,
    registered: Boolean,
    authenticated: Boolean,
    existingTrack: Boolean,
) {
    requireEntryContextState(
        type = entryType,
        feature = ENTRY_TRACKING_FEATURE_ID,
        integration = EntryTrackingIntegration.SYNCHRONIZATION.id,
        behaviorProjections = listOf(EntryTrackingBehavior.PROGRESS_SYNCHRONIZATION.id),
        evidence = listOf(
            contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, registered),
            contextEvidence(ENTRY_TRACKING_AUTHENTICATED_SUPPORT, authenticated),
            contextEvidence(ENTRY_TRACKING_AUTHENTICATED_TRACK, existingTrack),
        ),
        applicable = registered && authenticated && existingTrack,
    )
}

internal fun EntryTrackingHostService.toDescriptor(): EntryTrackingServiceDescriptor {
    return EntryTrackingServiceDescriptor(
        id = EntryTrackingServiceId(id),
        name = name,
        logoResource = logoResource,
        capabilities = EntryTrackingServiceCapabilities(
            statuses = capabilities.statuses.map { status -> EntryTrackingStatus(status.value, status.label) },
            scores = capabilities.scores,
            supportsReadingDates = capabilities.supportsReadingDates,
            supportsPrivateTracking = capabilities.supportsPrivateTracking,
            supportsRemoteDeletion = capabilities.supportsRemoteDeletion,
            supportsAutomaticBinding = capabilities.supportsAutomaticBinding,
        ),
    )
}
