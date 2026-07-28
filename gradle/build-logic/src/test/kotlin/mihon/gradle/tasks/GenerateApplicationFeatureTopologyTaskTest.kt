package mihon.gradle.tasks

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

class GenerateApplicationFeatureTopologyTaskTest {

    @Test
    fun `owner-local descriptors generate deterministic direct references`() {
        val source = generateApplicationFeatureProductionTopology(
            variantName = "debug",
            modules = listOf(
                module("translation.zeta", "example.ZetaApplicationFeatureModule"),
                module("translation.alpha", "example.AlphaApplicationFeatureModule"),
            ),
        )

        (
            source.indexOf("example.AlphaApplicationFeatureModule") <
                source.indexOf("example.ZetaApplicationFeatureModule")
            ) shouldBe true
        source shouldContain "List<ApplicationFeatureRuntimeModule>"
        source shouldNotContain "ServiceLoader"
    }

    @Test
    fun `an empty application Feature topology remains valid`() {
        val source = generateApplicationFeatureProductionTopology(
            variantName = "foss",
            modules = emptyList(),
        )

        source shouldContain "productionApplicationFeatureRuntimeModules"
        source shouldContain "productionApplicationFeatureRuntimeComponents"
        source shouldContain "listOf("
    }

    @Test
    fun `active variant components generate deterministic direct references`() {
        val source = generateApplicationFeatureProductionTopology(
            variantName = "debug",
            modules = emptyList(),
            components = listOf(
                component("translation.zeta", "example.ZetaTranslationRuntimeComponent"),
                component("translation.alpha", "example.AlphaTranslationRuntimeComponent"),
            ),
        )

        (
            source.indexOf("example.AlphaTranslationRuntimeComponent") <
                source.indexOf("example.ZetaTranslationRuntimeComponent")
            ) shouldBe true
        source shouldContain "ApplicationFeatureRuntimeComponents"
        source shouldContain "RegisteredApplicationFeatureRuntimeComponent"
        source shouldNotContain "ServiceLoader"
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

    @Test
    fun `duplicate or malformed runtime components fail generation`() {
        shouldThrow<GradleException> {
            generateApplicationFeatureProductionTopology(
                variantName = "debug",
                modules = emptyList(),
                components = listOf(
                    component("translation.same", "example.FirstTranslationRuntimeComponent"),
                    component("translation.same", "example.SecondTranslationRuntimeComponent"),
                ),
            )
        }.message shouldContain "Duplicate Application Feature runtime component descriptor id"

        shouldThrow<GradleException> {
            generateApplicationFeatureProductionTopology(
                variantName = "debug",
                modules = emptyList(),
                components = listOf(component("translation.valid", "not-qualified")),
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
    ) = ApplicationFeatureRuntimeComponentDescriptor(id, symbol, "$id.component-descriptor")
}
