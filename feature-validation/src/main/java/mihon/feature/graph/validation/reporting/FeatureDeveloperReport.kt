package mihon.feature.graph.validation.reporting

/** Neutral, deterministic snapshot of discovered and evaluated feature participation. */
data class FeatureDeveloperReport(
    val contentTypes: List<FeatureDeveloperContentType>,
    val features: List<FeatureDeveloperFeature>,
    val executionPoints: List<FeatureDeveloperExecutionPoint>,
    val executionParticipants: List<FeatureDeveloperExecutionParticipant>,
    val integrations: List<FeatureDeveloperIntegration>,
    val obligations: List<FeatureDeveloperObligation>,
)

data class FeatureDeveloperContentType(
    val id: String,
    val owner: String,
    val providers: List<FeatureDeveloperOwnedReference>,
    val specializedAdapters: List<FeatureDeveloperOwnedReference>,
    val contractFixtures: List<FeatureDeveloperOwnedReference>,
)

data class FeatureDeveloperFeature(
    val id: String,
    val owner: String,
)

data class FeatureDeveloperExecutionPoint(
    val id: String,
    val owner: String,
    val eventType: String,
    val phase: String,
    val failurePolicy: String,
)

data class FeatureDeveloperExecutionParticipant(
    val contentType: FeatureDeveloperOwnedReference,
    val point: FeatureDeveloperOwnedReference,
    val participant: FeatureDeveloperOwnedReference,
    val state: FeatureDeveloperIntegrationState,
    val prerequisites: FeatureDeveloperCapabilityRequirement,
    val contextInputs: List<FeatureDeveloperOwnedReference>,
    val after: List<String>,
    val before: List<String>,
    val contracts: List<FeatureDeveloperExecutionContract>,
)

data class FeatureDeveloperExecutionContract(
    val id: String,
    val validations: List<FeatureDeveloperContractValidation>,
)

data class FeatureDeveloperOwnedReference(
    val id: String,
    val owner: String,
)

data class FeatureDeveloperIntegration(
    val contentType: FeatureDeveloperOwnedReference,
    val feature: FeatureDeveloperOwnedReference,
    val id: String,
    val state: FeatureDeveloperIntegrationState,
    val prerequisites: FeatureDeveloperCapabilityRequirement,
    val matchedProviders: List<FeatureDeveloperOwnedReference>,
    val unmetPrerequisites: List<FeatureDeveloperCapabilityRequirement>,
    val missingSpecializedPrerequisites: List<FeatureDeveloperOwnedReference>,
    val suppliedSpecializedAdapters: List<FeatureDeveloperOwnedReference>,
    val pendingSpecializedRequirements: List<FeatureDeveloperOwnedReference>,
    val contextInputs: List<FeatureDeveloperOwnedReference>,
    val declaredBlockers: List<FeatureDeveloperBlocker>,
    val behaviors: List<FeatureDeveloperArtifact>,
    val contracts: List<FeatureDeveloperContract>,
    val projections: List<FeatureDeveloperProjection>,
)

enum class FeatureDeveloperIntegrationState {
    INAPPLICABLE,
    CONDITIONAL,
    INCOMPLETE,
    APPLICABLE,
}

sealed interface FeatureDeveloperCapabilityRequirement {
    data object Always : FeatureDeveloperCapabilityRequirement

    data class Provided(
        val capability: FeatureDeveloperOwnedReference,
    ) : FeatureDeveloperCapabilityRequirement

    data class AllOf(
        val terms: List<FeatureDeveloperCapabilityRequirement>,
    ) : FeatureDeveloperCapabilityRequirement

    data class AnyOf(
        val terms: List<FeatureDeveloperCapabilityRequirement>,
    ) : FeatureDeveloperCapabilityRequirement
}

data class FeatureDeveloperBlocker(
    val id: String,
    val inputs: List<FeatureDeveloperOwnedReference>,
)

data class FeatureDeveloperArtifact(
    val id: String,
    val availability: FeatureDeveloperArtifactAvailability,
)

enum class FeatureDeveloperArtifactAvailability {
    NOT_SELECTED,
    CONDITIONAL,
    BLOCKED,
    SELECTED,
}

data class FeatureDeveloperContract(
    val id: String,
    val availability: FeatureDeveloperArtifactAvailability,
    val fixtureRequirements: List<FeatureDeveloperOwnedReference>,
    val validations: List<FeatureDeveloperContractValidation>,
)

data class FeatureDeveloperContractValidation(
    val scenario: String?,
    val outcome: FeatureDeveloperContractValidationOutcome,
    val details: List<String>,
)

enum class FeatureDeveloperContractValidationOutcome {
    PASSED,
    FAILED,
    CRASHED,
    INVALID_SCENARIO,
}

data class FeatureDeveloperProjection(
    val id: String,
    val owner: String,
    val implementationPresent: Boolean,
    val availability: FeatureDeveloperArtifactAvailability,
)

data class FeatureDeveloperObligation(
    val responsibleOwner: String,
    val category: FeatureDeveloperObligationCategory,
    val subjects: List<FeatureDeveloperSubject>,
    val artifact: String,
    val details: String,
)

data class FeatureDeveloperSubject(
    val contentType: String,
    val feature: String,
    val integration: String,
)

enum class FeatureDeveloperObligationCategory {
    SPECIALIZED_ADAPTER,
    EXECUTION_SPECIALIZED_ADAPTER,
    CONTRACT_FIXTURE,
    PROJECTION,
    CONTRACT_VERIFIER,
    CONTRACT_SCENARIO,
    INVALID_CONTRACT_SCENARIO,
}
