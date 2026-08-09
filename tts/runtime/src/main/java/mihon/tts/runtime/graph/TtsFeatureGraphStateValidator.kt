package mihon.tts.runtime.graph

import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureSubjectId

internal class TtsFeatureGraphStateValidator(
    private val evaluation: FeatureGraphEvaluation,
) {
    fun validate() {
        val integration = evaluation.integrations.singleOrNull { candidate ->
            candidate.subject.affectedSubject.id == FeatureSubjectId.Application &&
                candidate.subject.feature == TTS_FEATURE_ID &&
                candidate.subject.integration == TTS_ENGINE_REGISTRY_INTEGRATION_ID
        }
        check(integration is ApplicableFeatureIntegration) {
            "TTS application integration must be applicable, but resolved as $integration"
        }
    }
}
