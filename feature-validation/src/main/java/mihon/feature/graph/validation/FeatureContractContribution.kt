package mihon.feature.graph.validation

import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId
import java.util.ServiceLoader

/** Identity reference to one exact behavioral contract definition declared by a production feature contribution. */
class FeatureContractReference(
    val feature: FeatureId,
    val contract: FeatureBehaviorContract,
) {
    override fun equals(other: Any?): Boolean {
        return other is FeatureContractReference &&
            feature == other.feature &&
            contract === other.contract
    }

    override fun hashCode(): Int = 31 * feature.hashCode() + System.identityHashCode(contract)

    override fun toString(): String = "FeatureContractReference(feature=$feature, contract=${contract.id})"
}

/** Identity reference to a behavioral contract declared by one executable participant. */
class FeatureExecutionContractReference(
    val participant: FeatureExecutionParticipantId,
    val contract: FeatureBehaviorContract,
) {
    override fun equals(other: Any?): Boolean {
        return other is FeatureExecutionContractReference &&
            participant == other.participant &&
            contract === other.contract
    }

    override fun hashCode(): Int = 31 * participant.hashCode() + System.identityHashCode(contract)

    override fun toString(): String {
        return "FeatureExecutionContractReference(participant=$participant, contract=${contract.id})"
    }
}

/** Framework-neutral executable validation owned by the feature that declared [contract]. */
data class FeatureContractVerifier(
    val contract: FeatureContractReference,
    val verification: FeatureContractVerification,
)

fun interface FeatureContractVerification {
    suspend fun verify(input: FeatureContractExecutionInput): FeatureContractVerificationResult
}

data class FeatureExecutionContractVerifier(
    val contract: FeatureExecutionContractReference,
    val verification: FeatureExecutionContractVerification,
)

fun interface FeatureExecutionContractVerification {
    suspend fun verify(input: FeatureExecutionContractExecutionInput): FeatureContractVerificationResult
}

/** Typed evidence factory for one context in which a conditional contract must become applicable. */
fun interface FeatureContractEvidenceFactory {
    fun create(input: FeatureContractScenarioInput): List<ContextEvidence<*>>
}

data class FeatureContractScenario(
    val id: FeatureContractScenarioId,
    val contract: FeatureContractReference,
    val integration: FeatureIntegrationId,
    val evidenceFactory: FeatureContractEvidenceFactory,
)

data class FeatureExecutionContractScenario(
    val id: FeatureContractScenarioId,
    val contract: FeatureExecutionContractReference,
    val evidenceFactory: FeatureContractEvidenceFactory,
)

/** Validation-only owner contribution discovered from the validation classpath. */
interface FeatureValidationContributor {
    val owner: ContributionOwner

    fun contributeTo(sink: FeatureValidationContributionSink)
}

fun featureValidationContributor(
    owner: ContributionOwner,
    contribute: FeatureValidationContributionSink.() -> Unit,
): FeatureValidationContributor {
    return object : FeatureValidationContributor {
        override val owner = owner

        override fun contributeTo(sink: FeatureValidationContributionSink) {
            sink.contribute()
        }
    }
}

class FeatureValidationContributionSink internal constructor(
    private val owner: ContributionOwner,
) {
    private val verifiers = mutableListOf<OwnedFeatureContractVerifier>()
    private val scenarios = mutableListOf<OwnedFeatureContractScenario>()
    private val executionVerifiers = mutableListOf<OwnedFeatureExecutionContractVerifier>()
    private val executionScenarios = mutableListOf<OwnedFeatureExecutionContractScenario>()

    fun add(verifier: FeatureContractVerifier) {
        verifiers += OwnedFeatureContractVerifier(owner, verifier)
    }

    fun add(scenario: FeatureContractScenario) {
        scenarios += OwnedFeatureContractScenario(owner, scenario)
    }

    fun add(verifier: FeatureExecutionContractVerifier) {
        executionVerifiers += OwnedFeatureExecutionContractVerifier(owner, verifier)
    }

    fun add(scenario: FeatureExecutionContractScenario) {
        executionScenarios += OwnedFeatureExecutionContractScenario(owner, scenario)
    }

    internal fun snapshot(): DiscoveredFeatureValidationContributions {
        return DiscoveredFeatureValidationContributions(
            verifiers = verifiers.toList(),
            scenarios = scenarios.toList(),
            executionVerifiers = executionVerifiers.toList(),
            executionScenarios = executionScenarios.toList(),
        )
    }
}

data class OwnedFeatureContractVerifier(
    val owner: ContributionOwner,
    val verifier: FeatureContractVerifier,
)

data class OwnedFeatureContractScenario(
    val owner: ContributionOwner,
    val scenario: FeatureContractScenario,
)

data class OwnedFeatureExecutionContractVerifier(
    val owner: ContributionOwner,
    val verifier: FeatureExecutionContractVerifier,
)

data class OwnedFeatureExecutionContractScenario(
    val owner: ContributionOwner,
    val scenario: FeatureExecutionContractScenario,
)

data class DiscoveredFeatureValidationContributions(
    val verifiers: List<OwnedFeatureContractVerifier>,
    val scenarios: List<OwnedFeatureContractScenario>,
    val executionVerifiers: List<OwnedFeatureExecutionContractVerifier> = emptyList(),
    val executionScenarios: List<OwnedFeatureExecutionContractScenario> = emptyList(),
)

fun discoverFeatureValidationContributions(
    contributors: Iterable<FeatureValidationContributor>,
): DiscoveredFeatureValidationContributions {
    val snapshots = contributors.map { contributor ->
        FeatureValidationContributionSink(contributor.owner)
            .also(contributor::contributeTo)
            .snapshot()
    }
    return DiscoveredFeatureValidationContributions(
        verifiers = snapshots.flatMap { it.verifiers },
        scenarios = snapshots.flatMap { it.scenarios },
        executionVerifiers = snapshots.flatMap { it.executionVerifiers },
        executionScenarios = snapshots.flatMap { it.executionScenarios },
    )
}

/** Discovers independently packaged validation contributors without a central contract or feature list. */
fun loadFeatureValidationContributors(
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): List<FeatureValidationContributor> {
    return ServiceLoader.load(FeatureValidationContributor::class.java, classLoader).toList()
}
