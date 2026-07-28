package eu.kanade.tachiyomi.validation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.validation.ProductionEntryInteractionValidationEnvironment
import mihon.feature.graph.validation.reporting.renderFeatureDeveloperReport
import mihon.feature.graph.validation.reporting.validateAndBuildFeatureDeveloperReport
import mihon.feature.runtime.productionApplicationFeatureRuntimeModules
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProductionFeatureDeveloperReportTest {
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
        val composition = environment.composition(productionApplicationFeatureRuntimeModules())
        val result = validateAndBuildFeatureDeveloperReport(
            graph = composition.featureGraph,
            evaluation = composition.featureGraphEvaluation,
        )
        val rendered = renderFeatureDeveloperReport(result.report)

        System.getProperty(REPORT_OUTPUT_PROPERTY)?.let { path ->
            val output = File(path)
            output.parentFile.mkdirs()
            output.writeText(rendered)
        }
        println(rendered)

        result.validation.isSuccessful shouldBe true
        result.report.application?.owner shouldBe "application-feature-runtime"
        result.report.integrations.isNotEmpty() shouldBe true
        result.report.obligations shouldBe emptyList()
        rendered shouldContain "Katari feature developer report"
        rendered shouldContain "Application Features"
        rendered shouldContain "Contextual validation scenarios are samples"
    }

    private companion object {
        const val REPORT_OUTPUT_PROPERTY = "mihon.feature.report.output"
    }
}
