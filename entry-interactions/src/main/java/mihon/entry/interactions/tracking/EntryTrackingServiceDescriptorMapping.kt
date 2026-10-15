package mihon.entry.interactions.tracking

import mihon.entry.interactions.tracking.host.EntryTrackingHostService

internal fun EntryTrackingHostService.toDescriptor(): EntryTrackingServiceDescriptor {
    return EntryTrackingServiceDescriptor(
        id = EntryTrackingServiceId(id),
        name = name,
        logoResource = logoResource,
        capabilities = EntryTrackingServiceCapabilities(
            statuses = capabilities.statuses.map { status ->
                EntryTrackingStatus(
                    status.value,
                    status.label,
                )
            },
            scores = capabilities.scores,
            supportsReadingDates = capabilities.supportsReadingDates,
            supportsPrivateTracking = capabilities.supportsPrivateTracking,
            supportsRemoteDeletion = capabilities.supportsRemoteDeletion,
            supportsAutomaticBinding = capabilities.supportsAutomaticBinding,
        ),
    )
}
