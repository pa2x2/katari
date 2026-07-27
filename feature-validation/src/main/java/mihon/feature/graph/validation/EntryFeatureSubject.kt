package mihon.feature.graph.validation

import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.FeatureExecutionParticipantSubject
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.FeatureSubjectId

internal val FeatureIntegrationSubject.entryContentType: ContentTypeId
    get() = affectedSubject.id.requireEntryContentType()

internal val FeatureExecutionParticipantSubject.entryContentType: ContentTypeId
    get() = affectedSubject.id.requireEntryContentType()

internal val FeatureGraph.entryContentTypes: List<ContentTypeContribution>
    get() = subjects.filterIsInstance<ContentTypeContribution>()

private fun FeatureSubjectId.requireEntryContentType(): ContentTypeId = when (this) {
    FeatureSubjectId.Application ->
        error("Expected an Entry content-type Feature subject, received $stableValue")
    is FeatureSubjectId.EntryContentType -> contentType
}
