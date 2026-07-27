package mihon.feature.runtime

import mihon.feature.graph.FeatureArtifactSelection
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.discoverAndAssembleFeatureGraph
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.selectFeatureArtifacts

/** Independently installed contributions consumed by the single application Feature runtime. */
data class FeatureRuntimeInputs(
    val graphContributors: List<FeatureGraphContributor> = emptyList(),
    val executionBindings: List<FeatureExecutionParticipantBinding<*>> = emptyList(),
    val durableExecutionBindings: List<FeatureDurableExecutionParticipantBinding<*>> = emptyList(),
)

/** The single runtime authority for all application and Entry-scoped Features. */
data class FeatureRuntimeComposition(
    val graph: FeatureGraph,
    val evaluation: FeatureGraphEvaluation,
    val artifacts: FeatureArtifactSelection,
    val executions: FeatureExecutionRuntime,
)

fun interface FeatureRuntimeWarmup {
    fun warmup()
}

fun createFeatureRuntimeComposition(
    inputs: Iterable<FeatureRuntimeInputs>,
): FeatureRuntimeComposition {
    val installed = inputs.toList()
    val graph = discoverAndAssembleFeatureGraph(installed.flatMap(FeatureRuntimeInputs::graphContributors))
    val evaluation = evaluateFeatureGraph(graph)
    return FeatureRuntimeComposition(
        graph = graph,
        evaluation = evaluation,
        artifacts = selectFeatureArtifacts(graph, evaluation),
        executions = FeatureExecutionRuntime(
            graph = graph,
            evaluation = evaluation,
            bindings = installed.flatMap(FeatureRuntimeInputs::executionBindings),
            durableBindings = installed.flatMap(FeatureRuntimeInputs::durableExecutionBindings),
        ),
    )
}
