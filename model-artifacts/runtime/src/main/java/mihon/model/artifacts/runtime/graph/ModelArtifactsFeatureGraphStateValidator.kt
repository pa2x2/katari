package mihon.model.artifacts.runtime.graph

import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureSubjectId

internal class ModelArtifactsFeatureGraphStateValidator(
    private val evaluation: FeatureGraphEvaluation,
) {
    fun validate() {
        val integration = evaluation.integrations.singleOrNull { candidate ->
            candidate.subject.affectedSubject.id == FeatureSubjectId.Application &&
                candidate.subject.feature == MODEL_ARTIFACTS_FEATURE_ID &&
                candidate.subject.integration == MODEL_ARTIFACTS_STORE_INTEGRATION_ID
        }
        check(integration is ApplicableFeatureIntegration) {
            "Model artifacts application integration must be applicable, but resolved as $integration"
        }
    }
}
