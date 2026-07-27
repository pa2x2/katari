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
        source shouldContain "listOf("
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
}
