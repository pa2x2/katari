package mihon.entry.interactions.media

import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.runtime.EntryInteractionProvider
import mihon.entry.interactions.runtime.entryInteractionCapability
import mihon.feature.graph.CapabilityId

/**
 * Operational type-owned media-session seam.
 *
 * Media runtimes use the same instance they contribute. Its presence is the authoritative fact that the type emits
 * structured media-session events and therefore participates in every applicable shared consequence.
 */
interface EntryMediaSessionProcessor :
    EntryInteractionProvider,
    EntryMediaSessionEventSink

val EntryMediaSessionCapability =
    entryInteractionCapability<EntryMediaSessionProcessor>(
        id = CapabilityId("entry.media-session"),
    )
