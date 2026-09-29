package mihon.gradle.tasks

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class GenerateApplicationFeatureTopologyTaskTest {

    @Test
    fun `components limited to other variants are not registered`() {
        val components = listOf(
            component("text-recognition.proprietary", "example.ProprietaryComponent", setOf("debug", "release")),
            component("text-recognition.catalog", "example.CatalogOnlyComponent", setOf("foss")),
            component("text-recognition.everywhere", "example.EverywhereComponent"),
        )

        val foss = generateApplicationFeatureProductionTopology("foss", emptyList(), components)
        val release = generateApplicationFeatureProductionTopology("release", emptyList(), components)

        foss shouldNotContain "example.ProprietaryComponent"
        foss shouldContain "example.CatalogOnlyComponent"
        foss shouldContain "example.EverywhereComponent"
        release shouldContain "example.ProprietaryComponent"
        release shouldNotContain "example.CatalogOnlyComponent"
    }

    private fun component(
        id: String,
        symbol: String,
        variants: Set<String>? = null,
    ) = ApplicationFeatureRuntimeComponentDescriptor(id, symbol, "$id.component-descriptor", variants)
}
