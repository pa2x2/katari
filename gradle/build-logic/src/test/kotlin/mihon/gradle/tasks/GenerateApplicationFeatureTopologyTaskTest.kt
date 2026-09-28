package mihon.gradle.tasks

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

class GenerateApplicationFeatureTopologyTaskTest {

    @Test
    fun `owner-local module and component descriptors generate deterministic direct references`() {
        val source = generateApplicationFeatureProductionTopology(
            variantName = "debug",
            modules = listOf(
                module("translation.zeta", "example.ZetaApplicationFeatureModule"),
                module("translation.alpha", "example.AlphaApplicationFeatureModule"),
            ),
            components = listOf(
                component("translation.zeta", "example.ZetaTranslationRuntimeComponent"),
                component("translation.alpha", "example.AlphaTranslationRuntimeComponent"),
            ),
        )

        (
            source.indexOf("example.AlphaApplicationFeatureModule") <
                source.indexOf("example.ZetaApplicationFeatureModule")
            ) shouldBe true
        (
            source.indexOf("example.AlphaTranslationRuntimeComponent") <
                source.indexOf("example.ZetaTranslationRuntimeComponent")
            ) shouldBe true
        source shouldNotContain "ServiceLoader"
    }

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

    @Test
    fun `duplicate ids and symbols fail generation`() {
        shouldThrow<GradleException> {
            generateApplicationFeatureProductionTopology(
                variantName = "debug",
                modules = listOf(
                    module("translation.same", "example.FirstApplicationFeatureModule"),
                    module("translation.same", "example.SecondApplicationFeatureModule"),
                ),
            )
        }.message shouldContain "Duplicate Application Feature descriptor id"

        shouldThrow<GradleException> {
            generateApplicationFeatureProductionTopology(
                variantName = "debug",
                modules = listOf(
                    module("translation.first", "example.SameApplicationFeatureModule"),
                    module("translation.second", "example.SameApplicationFeatureModule"),
                ),
            )
        }.message shouldContain "Duplicate Application Feature descriptor symbol"
    }

    @Test
    fun `malformed ids and symbols fail generation`() {
        shouldThrow<GradleException> {
            generateApplicationFeatureProductionTopology(
                variantName = "debug",
                modules = listOf(module("Translation invalid", "example.ValidApplicationFeatureModule")),
            )
        }.message shouldContain "invalid id"

        shouldThrow<GradleException> {
            generateApplicationFeatureProductionTopology(
                variantName = "debug",
                modules = listOf(module("translation.valid", "not-qualified")),
            )
        }.message shouldContain "invalid symbol"
    }

    private fun module(
        id: String,
        symbol: String,
    ) = ApplicationFeatureModuleDescriptor(id, symbol, "$id.descriptor")

    private fun component(
        id: String,
        symbol: String,
        variants: Set<String>? = null,
    ) = ApplicationFeatureRuntimeComponentDescriptor(id, symbol, "$id.component-descriptor", variants)
}
