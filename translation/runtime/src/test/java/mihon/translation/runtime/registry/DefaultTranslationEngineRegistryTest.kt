package mihon.translation.runtime.registry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelId
import mihon.translation.api.model.TranslationModelOperationResult
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.spi.contribution.TranslationEngineContribution
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import mihon.translation.spi.setup.TranslationEngineSetup
import mihon.translation.spi.setup.TranslationSetupResult
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

        override suspend fun inspectLanguageSupport() =
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.AnyLanguage,
            )

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

        override suspend fun openSetup() =
            TranslationSetupResult.Opened(TranslationSetupDestination.External)

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
