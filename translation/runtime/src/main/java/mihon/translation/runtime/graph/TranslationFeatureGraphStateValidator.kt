package mihon.translation.runtime.graph

import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureSubjectId

internal class TranslationFeatureGraphStateValidator(
    private val evaluation: FeatureGraphEvaluation,
) {
    fun validate() {
        val integration = evaluation.integrations.singleOrNull { candidate ->
            candidate.subject.affectedSubject.id == FeatureSubjectId.Application &&
                candidate.subject.feature == TRANSLATION_FEATURE_ID &&
                candidate.subject.integration == TRANSLATION_ENGINE_REGISTRY_INTEGRATION_ID
        }
        check(integration is ApplicableFeatureIntegration) {
            "Translation application integration must be applicable, but resolved as $integration"
        }
    }
}
