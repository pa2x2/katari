package mihon.entry.interactions.documentation

import mihon.entry.interactions.ENTRY_TRACKING_REGISTERED_SUPPORT
import mihon.entry.interactions.LEGACY_SOURCE_REGISTERED_SUPPORT_CONTEXT
import mihon.entry.interactions.LOCAL_SOURCE_REGISTERED_SUPPORT_CONTEXT
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.ContextInputDefinition
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.contextEvidence

data class EntryContentTypeReferenceRegistrations(
    val localSource: Set<ContentTypeId>,
    val legacyExtensions: Set<ContentTypeId>,
    val trackingServices: Set<ContentTypeId>,
)

fun interface EntryContentTypeReferenceContextEvidenceProvider {
    fun evidence(
        subject: FeatureIntegrationSubject,
        input: ContextInputDefinition<*>,
    ): ContextEvidence<*>?
}

fun entryContentTypeReferenceContextEvidence(
    registrations: EntryContentTypeReferenceRegistrations,
): EntryContentTypeReferenceContextEvidenceProvider {
    return EntryContentTypeReferenceContextEvidenceProvider { subject, input ->
        when (input) {
            LOCAL_SOURCE_REGISTERED_SUPPORT_CONTEXT -> contextEvidence(
                LOCAL_SOURCE_REGISTERED_SUPPORT_CONTEXT,
                subject.contentType in registrations.localSource,
            )
            LEGACY_SOURCE_REGISTERED_SUPPORT_CONTEXT -> contextEvidence(
                LEGACY_SOURCE_REGISTERED_SUPPORT_CONTEXT,
                subject.contentType in registrations.legacyExtensions,
            )
            ENTRY_TRACKING_REGISTERED_SUPPORT -> contextEvidence(
                ENTRY_TRACKING_REGISTERED_SUPPORT,
                subject.contentType in registrations.trackingServices,
            )
            else -> null
        }
    }
}
