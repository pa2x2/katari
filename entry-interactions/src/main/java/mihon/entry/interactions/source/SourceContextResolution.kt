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

internal fun FeatureGraphEvaluation.requireSourceContextState(
    feature: FeatureId,
    integration: FeatureIntegrationId,
    behaviorProjection: FeatureArtifactId,
    evidence: List<ContextEvidence<*>>,
    applicable: Boolean,
    contentType: EntryType? = null,
) {
    val subjects = integrations
        .map { it.subject }
        .filter { it.feature == feature && it.integration == integration }
        .filter { contentType == null || it.contentType == contentType.toContentTypeId() }
    check(subjects.isNotEmpty()) { "Source integration $feature:$integration was not discovered" }

    subjects.forEach { subject ->
        val resolution = resolveFeatureContext(
            evaluation = this,
            contentType = subject.contentType,
            feature = feature,
            integration = integration,
            evidence = evidence,
        )
        val hasBehavior = resolution.behaviorProjections.any { it.projection.id == behaviorProjection }
        val matches = if (applicable) {
            resolution.integration is ApplicableFeatureContext && hasBehavior
        } else {
            resolution.integration is BlockedFeatureContext && !hasBehavior
        }
        check(matches) {
            "Source integration $feature:$integration resolved inconsistently for ${subject.contentType}: " +
                "${resolution.integration}, behaviors=${resolution.behaviorProjections}"
        }
    }
}
