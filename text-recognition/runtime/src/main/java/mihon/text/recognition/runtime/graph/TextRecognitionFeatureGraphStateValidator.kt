package mihon.text.recognition.runtime.graph

import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureSubjectId

internal class TextRecognitionFeatureGraphStateValidator(
    private val evaluation: FeatureGraphEvaluation,
) {
    fun validate() {
        val integration = evaluation.integrations.singleOrNull { candidate ->
            candidate.subject.affectedSubject.id == FeatureSubjectId.Application &&
                candidate.subject.feature == TEXT_RECOGNITION_FEATURE_ID &&
                candidate.subject.integration == TEXT_RECOGNITION_PIPELINE_INTEGRATION_ID
        }
        check(integration is ApplicableFeatureIntegration) {
            "Text recognition application integration must be applicable, but resolved as $integration"
        }
    }
}
