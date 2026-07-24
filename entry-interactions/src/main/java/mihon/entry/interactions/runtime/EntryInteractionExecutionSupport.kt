package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionResult

internal fun FeatureExecutionResult.throwFirstFailure() {
    failures.firstOrNull()?.let { throw it.error }
}
