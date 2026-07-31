package mihon.entry.interactions.validation

import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.feature.graph.validation.FeatureContractValidationResult
import mihon.feature.graph.validation.reporting.FeatureDeveloperReportingResult
import mihon.feature.graph.validation.reporting.validateAndBuildFeatureDeveloperReport

/** Validation-only entry point over the exact composition produced by the application boundary. */
internal suspend fun validateEntryInteractionContracts(
    composition: EntryInteractionComposition,
    classLoader: ClassLoader = defaultEntryInteractionValidationClassLoader(),
): FeatureContractValidationResult {
    return evaluateEntryInteractionContracts(composition, classLoader).validation
}

/** Single validation-only evaluation used by contract gates and developer reporting. */
internal suspend fun evaluateEntryInteractionContracts(
    composition: EntryInteractionComposition,
    classLoader: ClassLoader = defaultEntryInteractionValidationClassLoader(),
): FeatureDeveloperReportingResult {
    return validateAndBuildFeatureDeveloperReport(
        graph = composition.featureGraph,
        evaluation = composition.featureGraphEvaluation,
        classLoader = classLoader,
    )
}

private fun defaultEntryInteractionValidationClassLoader(): ClassLoader =
    requireNotNull(Thread.currentThread().contextClassLoader) {
        "Entry interaction contract validation requires a thread context class loader"
    }
