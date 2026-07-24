package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.ApplicableFeatureContext
import mihon.feature.graph.BlockedFeatureContext
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.resolveFeatureContext

internal fun FeatureGraphEvaluation.requireEntryContextState(
    type: EntryType,
    feature: FeatureId,
    integration: FeatureIntegrationId,
    behaviorProjections: Collection<FeatureArtifactId>,
    evidence: List<ContextEvidence<*>>,
    applicable: Boolean,
) {
    val resolution = resolveFeatureContext(
        evaluation = this,
        contentType = type.toContentTypeId(),
        feature = feature,
        integration = integration,
        evidence = evidence,
    )
    val resolvedBehavior = resolution.behaviorProjections.mapTo(mutableSetOf()) { it.projection.id }
    val expectedBehavior = behaviorProjections.toSet()
    val matches = if (applicable) {
        resolution.integration is ApplicableFeatureContext && resolvedBehavior == expectedBehavior
    } else {
        resolution.integration is BlockedFeatureContext && resolvedBehavior.isEmpty()
    }
    check(matches) {
        "Entry integration $feature:$integration resolved inconsistently for $type: " +
            "${resolution.integration}, behavior=$resolvedBehavior, expected=$expectedBehavior"
    }
}
