package mihon.entry.interactions.validation

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.validation.CompletedFeatureContractExecution
import mihon.feature.graph.validation.CompletedFeatureExecutionContractExecution
import mihon.feature.graph.validation.FeatureContractVerificationResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProductionEntryInteractionContractValidationTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private lateinit var environment: ProductionEntryInteractionValidationEnvironment

    @BeforeEach
    fun setUp() {
        environment = ProductionEntryInteractionValidationEnvironment(temporaryDirectory)
    }

    @AfterEach
    fun tearDown() {
        environment.close()
    }

    @Test
    fun `production composition executes every discovered contract without unresolved work`() = runTest {
        val composition = environment.composition()

        val result = validateEntryInteractionContracts(composition)
        val unexpectedContractExecutions = result.executions.filterNot { execution ->
            execution is CompletedFeatureContractExecution &&
                execution.verification == FeatureContractVerificationResult.Passed
        }
        val unexpectedParticipantExecutions = result.executionParticipantExecutions.filterNot { execution ->
            execution is CompletedFeatureExecutionContractExecution &&
                execution.verification == FeatureContractVerificationResult.Passed
        }
        val diagnostics = buildList {
            addAll(result.plan.issues.map { issue -> "Plan issue: $issue" })
            addAll(unexpectedContractExecutions.map { execution -> "Contract execution: $execution" })
            addAll(unexpectedParticipantExecutions.map { execution -> "Participant execution: $execution" })
        }

        withClue(diagnostics.joinToString(separator = "\n")) {
            result.isSuccessful shouldBe true
        }
        result.plan.isComplete shouldBe true
        result.plan.executions.isNotEmpty() shouldBe true
        unexpectedContractExecutions shouldBe emptyList()
        unexpectedParticipantExecutions shouldBe emptyList()
    }
}
