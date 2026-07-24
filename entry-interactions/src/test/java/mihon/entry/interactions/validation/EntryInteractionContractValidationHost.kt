package mihon.entry.interactions.validation

import mihon.entry.interactions.EntryInteractionComposition
import mihon.feature.graph.validation.FeatureContractValidationResult
import mihon.feature.graph.validation.reporting.FeatureDeveloperReportingResult
import mihon.feature.graph.validation.reporting.validateAndBuildFeatureDeveloperReport

/** Validation-only entry point over the exact composition produced by the application boundary. */
internal suspend fun validateEntryInteractionContracts(
    composition: EntryInteractionComposition,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): FeatureContractValidationResult {
    return evaluateEntryInteractionContracts(composition, classLoader).validation
}

/** Single validation-only evaluation used by contract gates and developer reporting. */
internal suspend fun evaluateEntryInteractionContracts(
    composition: EntryInteractionComposition,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): FeatureDeveloperReportingResult {
    return validateAndBuildFeatureDeveloperReport(
        graph = composition.featureGraph,
        evaluation = composition.featureGraphEvaluation,
        classLoader = classLoader,
    )
}
