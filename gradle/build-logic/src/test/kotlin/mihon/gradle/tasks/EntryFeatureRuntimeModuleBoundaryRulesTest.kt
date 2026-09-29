package mihon.gradle.tasks

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class EntryFeatureRuntimeModuleBoundaryRulesTest {
    @Test
    fun `graph-only production view used outside validation boundary fails`() {
        val findings = check(
            featureSource = "val graph = productionEntryFeatureGraphForValidation()",
        )

        findings.shouldHaveSize(1)
        findings.single().reason shouldContain "validation-only"
    }

    private fun check(
        featureSource: String,
    ): List<EntryFeatureRuntimeModuleBoundaryFinding> {
        return checkEntryFeatureRuntimeModuleBoundaries(
            listOf(
                EntryFeatureRuntimeModuleBoundarySource(
                    "entry-interactions/src/main/java/mihon/entry/interactions/example/Example.kt",
                    featureSource,
                ),
                EntryFeatureRuntimeModuleBoundarySource(
                    "entry-interactions/src/main/java/mihon/entry/interactions/runtime/" +
                        "EntryInteractionProductionGraphValidation.kt",
                    "fun productionEntryFeatureGraphForValidation() = emptyList<FeatureGraphContributor>()",
                ),
            ),
        )
    }
}
