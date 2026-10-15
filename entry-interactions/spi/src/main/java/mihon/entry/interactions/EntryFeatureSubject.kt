package mihon.entry.interactions

import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.FeatureContextEvaluation
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.FeatureSubjectId
import mihon.feature.graph.execution.FeatureExecutionParticipantSubject
import mihon.feature.graph.resolveFeatureContext as resolveSubjectFeatureContext

/** Entry-boundary projection of a generic Feature integration subject. */
val FeatureIntegrationSubject.entryContentType: ContentTypeId
    get() = affectedSubject.id.requireEntryContentType()

/** Entry-boundary projection of a generic Feature execution subject. */
val FeatureExecutionParticipantSubject.entryContentType: ContentTypeId
    get() = affectedSubject.id.requireEntryContentType()

/** Entry-only view of installed subjects. */
val FeatureGraph.entryContentTypes: List<ContentTypeContribution>
    get() = subjects.filterIsInstance<ContentTypeContribution>()

fun resolveFeatureContext(
    evaluation: FeatureGraphEvaluation,
    contentType: ContentTypeId,
    feature: FeatureId,
    integration: FeatureIntegrationId,
    evidence: Iterable<ContextEvidence<*>>,
): FeatureContextEvaluation = resolveSubjectFeatureContext(
    evaluation = evaluation,
    subject = FeatureSubjectId.EntryContentType(contentType),
    feature = feature,
    integration = integration,
    evidence = evidence,
)

private fun FeatureSubjectId.requireEntryContentType(): ContentTypeId = when (this) {
    FeatureSubjectId.Application ->
        error("Expected an Entry content-type Feature subject, received $stableValue")
    is FeatureSubjectId.EntryContentType -> contentType
}
