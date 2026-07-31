package mihon.entry.interactions.runtime

import mihon.feature.graph.execution.FeatureExecutionResult

internal fun FeatureExecutionResult.throwFirstFailure() {
    failures.firstOrNull()?.let { throw it.error }
}
