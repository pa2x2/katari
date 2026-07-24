package eu.kanade.tachiyomi.documentation

import mihon.entry.interactions.documentation.rendering.renderEntrySourceSdkConsumerCoverageMarkdown
import mihon.entry.interactions.documentation.rendering.replaceEntrySourceSdkConsumerCoverageGeneratedRegion
import mihon.entry.interactions.documentation.rendering.undocumentedEntrySourceSdkContracts
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProductionEntrySourceSdkConsumerCoverageDocumentationTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `generate or verify production source SDK consumer coverage`() {
        val mode = System.getProperty(MODE_PROPERTY)
        assumeTrue(mode != null, "Executed only by the source-SDK-consumer-coverage documentation tasks")
        val capabilityFile = File(requireNotNull(System.getProperty(FILE_PROPERTY)))

        ProductionEntryDocumentationEnvironment(temporaryDirectory).use { environment ->
            val plan = environment.sourceSdkConsumerCoveragePlan()
            val current = capabilityFile.readText()
            val undocumentedContracts = undocumentedEntrySourceSdkContracts(current, plan)
            check(undocumentedContracts.isEmpty()) {
                "Contextually consumed source SDK contracts are missing from handwritten contract documentation: " +
                    undocumentedContracts.joinToString()
            }
            val generated = renderEntrySourceSdkConsumerCoverageMarkdown(plan)
            val expected = replaceEntrySourceSdkConsumerCoverageGeneratedRegion(current, generated)
            when (mode) {
                GENERATE_MODE -> capabilityFile.writeText(expected)
                VERIFY_MODE -> check(current == expected) {
                    "Source SDK consumer coverage is stale. Run ./gradlew generateSourceSdkConsumerCoverage."
                }
                else -> error("Unknown source-SDK-consumer-coverage documentation mode: $mode")
            }
        }
    }

    private companion object {
        const val MODE_PROPERTY = "mihon.entry.sourceSdkConsumerCoverage.mode"
        const val FILE_PROPERTY = "mihon.entry.sourceSdkConsumerCoverage.file"
        const val GENERATE_MODE = "generate"
        const val VERIFY_MODE = "verify"
    }
}
