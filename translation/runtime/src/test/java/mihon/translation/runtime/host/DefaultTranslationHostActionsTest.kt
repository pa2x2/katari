package mihon.translation.runtime

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DefaultTranslationHostActionsTest {
    @Test
    fun `device inspection maps request-independent engine states`() = runTest {
        val cases = listOf(
            TranslationEngineDeviceAvailability.Available to TranslationDeviceAvailability.Available,
            TranslationEngineDeviceAvailability.UnsupportedOs(31) to
                TranslationDeviceAvailability.UnsupportedOs(31),
            TranslationEngineDeviceAvailability.ServiceMissing to
                TranslationDeviceAvailability.TranslationServiceMissing,
            TranslationEngineDeviceAvailability.Unavailable("disabled") to
                TranslationDeviceAvailability.SelectedEngineUnavailable(ENGINE_ID, "disabled"),
            TranslationEngineDeviceAvailability.Failed("OEM failure") to
                TranslationDeviceAvailability.ProviderFailure(ENGINE_ID, "OEM failure"),
        )

        cases.forEach { (engineState, expected) ->
            val engine = FakeEngine(engineState)
            actions(engine = engine).deviceAvailability() shouldBe expected
            engine.inspectionCount shouldBe 1
            engine.preparationCount shouldBe 0
        }
    }

    @Test
    fun `missing selected engines remain explicit and never fall back`() = runTest {
        actions(engine = null, known = listOf(KNOWN_ENGINE)).deviceAvailability() shouldBe
            TranslationDeviceAvailability.SelectedEngineUnavailable(ENGINE_ID)
        actions(engine = null, known = emptyList()).deviceAvailability() shouldBe
            TranslationDeviceAvailability.SelectedEngineMissing(ENGINE_ID)
    }

    private fun actions(
        engine: FakeEngine?,
        known: List<KnownTranslationEngine> = listOf(KNOWN_ENGINE),
    ): DefaultTranslationHostActions {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), ENGINE_ID)
        val registry = DefaultTranslationEngineRegistry(
            engines = listOfNotNull(engine),
            knownEngines = known,
        )
        return DefaultTranslationHostActions(
            preferences = preferences,
            engineRegistry = registry,
            knownEngineCatalog = registry,
            setupRegistry = DefaultTranslationEngineSetupRegistry(emptyList()),
        )
    }

    private class FakeEngine(
        private val availability: TranslationEngineDeviceAvailability,
    ) : TranslationEngine {
        override val catalogEntry = KNOWN_ENGINE
        override val presentation = PRESENTATION
        override val maximumInputCodePoints: Int? = null
        var inspectionCount = 0
        var preparationCount = 0

        override suspend fun inspectDevice(): TranslationEngineDeviceAvailability {
            inspectionCount++
            return availability
        }

        override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation {
            preparationCount++
            error("Device inspection must not prepare a Translation request")
        }

        override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation =
            error("Not used")

        override suspend fun translate(ready: ReadyTranslationEngineRequest): TranslationEngineExecution =
            error("Not used")
    }

    private companion object {
        val ENGINE_ID = TranslationEngineId("test-engine")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = TranslationProviderId("test"),
            providerName = "Test",
            engineName = "Test engine",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        val KNOWN_ENGINE = KnownTranslationEngine(
            id = ENGINE_ID,
            providerId = PRESENTATION.providerId,
            providerName = PRESENTATION.providerName,
            engineName = PRESENTATION.engineName,
            buildAvailability = TranslationEngineBuildAvailability.Included,
        )
    }
}
