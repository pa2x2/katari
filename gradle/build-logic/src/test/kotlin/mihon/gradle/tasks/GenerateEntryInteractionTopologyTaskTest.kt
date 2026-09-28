package mihon.gradle.tasks

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

class GenerateEntryInteractionTopologyTaskTest {

    @Test
    fun `owner-local descriptors generate deterministic feature and type topology`() {
        val source = generateEntryInteractionProductionTopology(
            variantName = "debug",
            featureModules = listOf(
                feature("entry.zeta", "example.ZetaFeatureRuntimeModule"),
                feature("entry.alpha", "example.AlphaFeatureRuntimeModule"),
            ),
            typeModules = listOf(
                type("manga", "example.mangaEntryTypeRuntimeModule"),
                type("anime", "example.animeEntryTypeRuntimeModule"),
            ),
        )

        (
            source.indexOf("example.AlphaFeatureRuntimeModule") <
                source.indexOf("example.ZetaFeatureRuntimeModule")
            ) shouldBe true
        (
            source.indexOf("example.animeEntryTypeRuntimeModule") <
                source.indexOf("example.mangaEntryTypeRuntimeModule")
            ) shouldBe true
        source shouldContain "Generated from owner-local runtime-module descriptors for variant debug"
        source shouldNotContain "ServiceLoader"
    }

    @Test
    fun `duplicate ids and symbols fail generation`() {
        shouldThrow<GradleException> {
            generateEntryInteractionProductionTopology(
                variantName = "debug",
                featureModules = listOf(
                    feature("entry.same", "example.FirstFeatureRuntimeModule"),
                    feature("entry.same", "example.SecondFeatureRuntimeModule"),
                ),
                typeModules = listOf(type("manga", "example.mangaEntryTypeRuntimeModule")),
            )
        }.message shouldContain "Duplicate Entry Feature descriptor id"

        shouldThrow<GradleException> {
            generateEntryInteractionProductionTopology(
                variantName = "debug",
                featureModules = listOf(
                    feature("entry.first", "example.SameFeatureRuntimeModule"),
                    feature("entry.second", "example.SameFeatureRuntimeModule"),
                ),
                typeModules = listOf(type("manga", "example.mangaEntryTypeRuntimeModule")),
            )
        }.message shouldContain "Duplicate Entry Feature descriptor symbol"
    }

    @Test
    fun `missing feature or type declarations fail generation`() {
        shouldThrow<GradleException> {
            generateEntryInteractionProductionTopology(
                variantName = "debug",
                featureModules = emptyList(),
                typeModules = listOf(type("manga", "example.mangaEntryTypeRuntimeModule")),
            )
        }.message shouldContain "No Entry Feature runtime-module descriptors"

        shouldThrow<GradleException> {
            generateEntryInteractionProductionTopology(
                variantName = "debug",
                featureModules = listOf(feature("entry.alpha", "example.AlphaFeatureRuntimeModule")),
                typeModules = emptyList(),
            )
        }.message shouldContain "No Entry type runtime-module descriptors"
    }

    @Test
    fun `malformed ids and symbols fail generation`() {
        shouldThrow<GradleException> {
            generateEntryInteractionProductionTopology(
                variantName = "debug",
                featureModules = listOf(feature("Entry invalid", "example.ValidFeatureRuntimeModule")),
                typeModules = listOf(type("manga", "example.mangaEntryTypeRuntimeModule")),
            )
        }.message shouldContain "invalid id"

        shouldThrow<GradleException> {
            generateEntryInteractionProductionTopology(
                variantName = "debug",
                featureModules = listOf(feature("entry.valid", "not-qualified")),
                typeModules = listOf(type("manga", "example.mangaEntryTypeRuntimeModule")),
            )
        }.message shouldContain "invalid symbol"
    }

    private fun feature(id: String, symbol: String) = EntryFeatureModuleDescriptor(id, symbol, "$id.descriptor")

    private fun type(id: String, symbol: String) = EntryTypeModuleDescriptor(id, symbol, "$id.descriptor")
}
