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

    private fun feature(id: String, symbol: String) = EntryFeatureModuleDescriptor(id, symbol, "$id.descriptor")

    private fun type(id: String, symbol: String) = EntryTypeModuleDescriptor(id, symbol, "$id.descriptor")
}
