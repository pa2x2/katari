package mihon.gradle.tasks

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntryContractValidationBoundaryRulesTest {

    @Test
    fun `feature owned validation may bind contracts without naming current types`() {
        val findings = check(
            path = "entry-interactions/src/test/java/mihon/entry/interactions/download/" +
                "EntryDownloadContractValidationContributor.kt",
            content = """
                class EntryDownloadContractValidationContributor {
                    val verifier = FeatureContractVerifier(FeatureContractReference(feature, contract)) { verify() }
                    val scenario = FeatureContractScenario(id, FeatureContractReference(feature, contract), integration) {
                        evidence()
                    }
                }
            """.trimIndent(),
        )

        findings.shouldBeEmpty()
    }

    @Test
    fun `contract validation cannot encode a current content type`() {
        val findings = check(
            path = "entry-interactions/src/test/java/mihon/entry/interactions/download/DownloadChecks.kt",
            content = "class DownloadChecks : FeatureValidationContributor { val fixture = EntryType.ANIME }",
        )

        assertEquals(1, findings.size)
        findings.single().reason shouldContain "graph-selected subjects"
    }

    @Test
    fun `ordinary tests cannot restate contract declarations or centralize suites`() {
        val findings = check(
            path = "entry-interactions/src/test/java/mihon/entry/interactions/download/EntryDownloadFeatureTest.kt",
            content = """
                val declared = integration.behavioralContracts
                val suites: Map<FeatureArtifactId, ContractSuite> = emptyMap()
            """.trimIndent(),
        )

        assertEquals(2, findings.size)
        findings.joinToString { it.reason } shouldContain "cannot inspect or select contract declarations"
        findings.joinToString { it.reason } shouldContain "central suite map"
    }

    @Test
    fun `production validation must use the single evaluated host`() {
        val findings = check(
            path = "entry-interactions/src/test/java/mihon/entry/interactions/validation/" +
                "ProductionEntryInteractionContractValidationTest.kt",
            content = "val result = validateFeatureContracts(planFeatureContractValidation(graph, evaluation, suites))",
        )

        findings.joinToString { it.reason } shouldContain "bypassing evaluated selection"
        findings.joinToString { it.reason } shouldContain "single evaluated Entry contract validation host"
    }

    @Test
    fun `validation host cannot dispatch a central contract suite`() {
        val findings = check(
            path = "entry-interactions/src/test/java/mihon/entry/interactions/validation/" +
                "EntryInteractionContractValidationHost.kt",
            content = "val verifier = when (contract.id) { else -> fallback }",
        )

        assertEquals(1, findings.size)
        findings.single().reason shouldContain "central suite switch"
    }

    @Test
    fun `validation contributor service contains every declaration exactly once`() {
        val contributorPath =
            "entry-interactions/src/test/java/mihon/entry/interactions/download/EntryDownloadChecks.kt"
        val findings = checkEntryContractValidationBoundaries(
            listOf(
                EntryContractValidationBoundarySource(
                    contributorPath,
                    """
                        package mihon.entry.interactions

                        class EntryDownloadChecks : FeatureValidationContributor
                    """.trimIndent(),
                ),
                EntryContractValidationBoundarySource(
                    FEATURE_VALIDATION_CONTRIBUTOR_SERVICE,
                    "mihon.entry.interactions.EntryDownloadChecks",
                ),
            ),
        )

        findings.shouldBeEmpty()
    }

    @Test
    fun `validation contributor cannot be omitted from service discovery`() {
        val findings = checkEntryContractValidationBoundaries(
            listOf(
                EntryContractValidationBoundarySource(
                    "entry-interactions/src/test/java/mihon/entry/interactions/EntryChecks.kt",
                    """
                        package mihon.entry.interactions

                        class EntryChecks : FeatureValidationContributor
                    """.trimIndent(),
                ),
                EntryContractValidationBoundarySource(FEATURE_VALIDATION_CONTRIBUTOR_SERVICE, ""),
            ),
        )

        assertEquals(1, findings.size)
        findings.single().reason shouldContain "missing from the service registry"
    }

    @Test
    fun `validation service cannot duplicate or invent contributors`() {
        val findings = checkEntryContractValidationBoundaries(
            listOf(
                EntryContractValidationBoundarySource(
                    "entry-interactions/src/test/java/mihon/entry/interactions/EntryChecks.kt",
                    """
                        package mihon.entry.interactions

                        class EntryChecks : FeatureValidationContributor
                    """.trimIndent(),
                ),
                EntryContractValidationBoundarySource(
                    FEATURE_VALIDATION_CONTRIBUTOR_SERVICE,
                    """
                        mihon.entry.interactions.EntryChecks
                        mihon.entry.interactions.EntryChecks
                        mihon.entry.interactions.UnknownChecks
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(2, findings.size)
        findings.joinToString { finding -> finding.reason } shouldContain "registered more than once"
        findings.joinToString { finding -> finding.reason } shouldContain "no declared contributor"
    }

    private fun check(path: String, content: String): List<EntryContractValidationBoundaryFinding> {
        return checkEntryContractValidationBoundaries(
            listOf(EntryContractValidationBoundarySource(path, content)),
        )
    }
}
