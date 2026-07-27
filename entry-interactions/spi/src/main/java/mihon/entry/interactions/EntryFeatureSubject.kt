package mihon.entry.interactions

import mihon.feature.graph.AfterCommitVolatileFeatureExecutionPointDefinition
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.DurableFeatureExecutionPointDefinition
import mihon.feature.graph.FeatureAfterCommitVolatileExecutionScope
import mihon.feature.graph.FeatureContextEvaluation
import mihon.feature.graph.FeatureDurableExecutionPreparationResult
import mihon.feature.graph.FeatureExecutionParticipantSubject
import mihon.feature.graph.FeatureExecutionResult
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.FeatureSubjectId
import mihon.feature.graph.FeatureTransactionalExecutionScope
import mihon.feature.graph.InlineFeatureExecutionPointDefinition
import mihon.feature.graph.TransactionalFeatureExecutionPointDefinition
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

suspend fun <E : Any> FeatureExecutionRuntime.executeInline(
    point: InlineFeatureExecutionPointDefinition<E>,
    contentType: ContentTypeId,
    event: E,
): FeatureExecutionResult = executeInline(point, FeatureSubjectId.EntryContentType(contentType), event)

suspend fun <E : Any> FeatureExecutionRuntime.prepareDurable(
    point: DurableFeatureExecutionPointDefinition<E>,
    contentType: ContentTypeId,
    event: E,
): FeatureDurableExecutionPreparationResult =
    prepareDurable(point, FeatureSubjectId.EntryContentType(contentType), event)

suspend fun <E : Any> FeatureTransactionalExecutionScope.execute(
    point: TransactionalFeatureExecutionPointDefinition<E>,
    contentType: ContentTypeId,
    event: E,
): FeatureExecutionResult = execute(point, FeatureSubjectId.EntryContentType(contentType), event)

suspend fun <E : Any> FeatureAfterCommitVolatileExecutionScope.execute(
    point: AfterCommitVolatileFeatureExecutionPointDefinition<E>,
    contentType: ContentTypeId,
    event: E,
): FeatureExecutionResult = execute(point, FeatureSubjectId.EntryContentType(contentType), event)

private fun FeatureSubjectId.requireEntryContentType(): ContentTypeId = when (this) {
    FeatureSubjectId.Application ->
        error("Expected an Entry content-type Feature subject, received $stableValue")
    is FeatureSubjectId.EntryContentType -> contentType
}
