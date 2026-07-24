package mihon.entry.interactions.validation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.validation.reporting.renderFeatureDeveloperReport
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProductionEntryInteractionDeveloperReportTest {
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
    fun `production composition renders the evaluated developer report`() = runTest {
        val result = evaluateEntryInteractionContracts(environment.composition())
        val rendered = renderFeatureDeveloperReport(result.report)

        System.getProperty(REPORT_OUTPUT_PROPERTY)?.let { path ->
            val output = File(path)
            output.parentFile.mkdirs()
            output.writeText(rendered)
        }
        println(rendered)

        result.validation.isSuccessful shouldBe true
        result.report.integrations.isNotEmpty() shouldBe true
        result.report.obligations shouldBe emptyList()
        rendered shouldContain "Katari feature developer report"
        rendered shouldContain "Contextual validation scenarios are samples"
    }

    private companion object {
        const val REPORT_OUTPUT_PROPERTY = "mihon.entry.feature.report.output"
    }
}
