package mihon.gradle.tasks

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntryContractValidationBoundaryRulesTest {

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
    fun `validation service must list exactly the declared contributors`() {
        val omitted = checkEntryContractValidationBoundaries(
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

        assertEquals(1, omitted.size)
        omitted.single().reason shouldContain "missing from the service registry"

        val inconsistent = checkEntryContractValidationBoundaries(
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

        assertEquals(2, inconsistent.size)
        inconsistent.joinToString { finding -> finding.reason } shouldContain "registered more than once"
        inconsistent.joinToString { finding -> finding.reason } shouldContain "no declared contributor"
    }

    private fun check(path: String, content: String): List<EntryContractValidationBoundaryFinding> {
        return checkEntryContractValidationBoundaries(
            listOf(EntryContractValidationBoundarySource(path, content)),
        )
    }
}
