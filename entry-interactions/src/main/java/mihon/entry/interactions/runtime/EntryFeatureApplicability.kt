package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId

internal inline fun <reified P : EntryInteractionProvider> FeatureGraphEvaluation.applicableProviderTypes(
    feature: FeatureId,
    integration: FeatureIntegrationId,
    behaviorProjection: FeatureArtifactId,
): Set<EntryType> {
    val applicableSubjects = behaviorProjections
        .asSequence()
        .filter { applicability ->
            applicability.subject.feature == feature &&
                applicability.subject.integration == integration &&
                applicability.projection.id == behaviorProjection
        }
        .map { it.subject }
        .toSet()

    return integrations
        .asSequence()
        .filterIsInstance<ApplicableFeatureIntegration>()
        .filter { it.subject in applicableSubjects }
        .flatMap { evaluated -> evaluated.matchedProviders.asSequence() }
        .mapNotNull { provider -> provider.implementation as? P }
        .mapTo(mutableSetOf(), EntryInteractionProvider::type)
}
