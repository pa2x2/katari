package mihon.feature.graph.validation.reporting

import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.validation.FeatureContractValidationResult
import mihon.feature.graph.validation.discoverAndPlanFeatureContractValidation
import mihon.feature.graph.validation.validateFeatureContracts

data class FeatureDeveloperReportingResult(
    val validation: FeatureContractValidationResult,
    val report: FeatureDeveloperReport,
)

suspend fun validateAndBuildFeatureDeveloperReport(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): FeatureDeveloperReportingResult {
    val validation = validateFeatureContracts(
        discoverAndPlanFeatureContractValidation(
            graph = graph,
            evaluation = evaluation,
            classLoader = classLoader,
        ),
    )
    return FeatureDeveloperReportingResult(
        validation = validation,
        report = buildFeatureDeveloperReport(graph, evaluation, validation),
    )
}
