package mihon.translation.runtime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineArtwork
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineDetails
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineContribution
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import mihon.translation.spi.TranslationEngineSetup
import mihon.translation.spi.TranslationSetupResult
import org.junit.jupiter.api.Test

class DefaultTranslationEngineRegistryTest {

    @Test
    fun `contributions determine catalog execution setup and deterministic order together`() {
        val late = FakeEngine("late")
        val early = FakeEngine("early")
        val excluded = knownEngine("excluded").copy(
            buildAvailability = TranslationEngineBuildAvailability.NotIncluded("Not in this build"),
        )
        val setup = FakeSetup(early.catalogEntry.id)
        val registry = DefaultTranslationEngineRegistry(
            listOf(
                TranslationEngineContribution(late, order = 10),
                TranslationEngineContribution(catalogEntry = excluded, order = 5),
                TranslationEngineContribution(early, setup = setup, order = -10),
            ),
        )

        registry.knownEngines.map { it.id.value } shouldContainExactly
            listOf("early", "excluded", "late")
        registry.engines shouldContainExactly listOf(early, late)
        registry.find(early.catalogEntry.id) shouldBe early
        registry.find(excluded.id) shouldBe null
        registry.findSetup(early.catalogEntry.id) shouldBe setup
    }

    @Test
    fun `duplicate provider ids fail registry construction`() {
        shouldThrow<IllegalArgumentException> {
            DefaultTranslationEngineRegistry(
                listOf(
                    TranslationEngineContribution(FakeEngine("same")),
                    TranslationEngineContribution(FakeEngine("same")),
                ),
            )
        }
    }

    private class FakeEngine(id: String) : TranslationEngine {
        override val catalogEntry = knownEngine(id)
        override val presentation = TranslationProviderPresentation(
            providerId = catalogEntry.providerId,
            providerName = catalogEntry.providerName,
            engineName = catalogEntry.engineName,
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        override val maximumInputCodePoints: Int? = null

        override suspend fun inspectDevice() = TranslationEngineDeviceAvailability.Available

        override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation =
            error("Not used")

        override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation =
            error("Not used")

        override suspend fun translate(ready: ReadyTranslationEngineRequest): TranslationEngineExecution =
            error("Not used")
    }

    private class FakeSetup(
        override val engine: TranslationEngineId,
    ) : TranslationEngineSetup {
        override val supportsSetup = true

        override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) = Unit

        override suspend fun openSetup() = TranslationSetupResult.Opened

        override suspend fun downloadModels(
            models: Set<TranslationModelId>,
            allowMeteredNetwork: Boolean,
        ) = TranslationModelOperationResult.Completed
    }

    private companion object {
        fun knownEngine(id: String) = KnownTranslationEngine(
            id = TranslationEngineId(id),
            providerId = TranslationProviderId(id),
            providerName = id,
            engineName = id,
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = TranslationEngineArtwork.Bundled(1),
            details = TranslationEngineDetails(
                description = "Test engine description",
                processingLocation = "Test processing location",
                privacyDescription = "Test privacy description",
            ),
        )
    }
}
