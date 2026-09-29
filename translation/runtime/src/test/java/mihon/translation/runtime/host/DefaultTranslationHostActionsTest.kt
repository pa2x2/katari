package mihon.translation.runtime.host

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.runtime.preference.ProfileTranslationPreferences
import mihon.translation.runtime.registry.DefaultTranslationEngineRegistry
import mihon.translation.runtime.selection.ProfileTranslationEngineResolver
import mihon.translation.spi.contribution.TranslationEngineContribution
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTranslationHostActionsTest {
    @Test
    fun `engine inspection timeout resolves loading when provider does not return`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val emissions = mutableListOf<TranslationEngineInspection>()
        val collection = backgroundScope.launch(dispatcher) {
            actions(
                engine = FakeEngine { suspendCoroutine { } },
                inspectionDispatcher = dispatcher,
                inspectionTimeoutMillis = 100,
            ).inspectEngineStates().toList(emissions)
        }
        runCurrent()

        emissions.single().engines.single().status shouldBe TranslationEngineStatus.Checking

        advanceTimeBy(100)
        runCurrent()

        emissions.last().engines.single().status shouldBe TranslationEngineStatus.Unavailable(
            TranslationUnavailableReason.EngineUnavailable(
                ENGINE_ID,
                "Engine availability check timed out",
            ),
        )
        collection.isCompleted shouldBe true
    }

    private fun actions(
        engine: FakeEngine,
        inspectionDispatcher: CoroutineDispatcher,
        inspectionTimeoutMillis: Long,
    ): DefaultTranslationHostActions {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), ENGINE_ID)
        preferences.engine.set(ENGINE_ID)
        val registry = DefaultTranslationEngineRegistry(
            contributions = listOf(TranslationEngineContribution(KNOWN_ENGINE, engine)),
        )
        return DefaultTranslationHostActions(
            preferences = preferences,
            engineRegistry = registry,
            knownEngineCatalog = registry,
            setupRegistry = registry,
            profileEngineResolver = ProfileTranslationEngineResolver(preferences, registry),
            defaultTargetResolver = { null },
            inspectionDispatcher = inspectionDispatcher,
            inspectionTimeoutMillis = inspectionTimeoutMillis,
        )
    }

    private class FakeEngine(
        private val inspectAvailability: suspend () -> TranslationEngineDeviceAvailability,
    ) : TranslationEngine {
        override val catalogEntry = KNOWN_ENGINE
        override val presentation = PRESENTATION
        override val maximumInputCodePoints: Int? = null

        override suspend fun inspectDevice() = inspectAvailability()

        override suspend fun inspectLanguageSupport() =
            TranslationLanguageSupportInspection.Available(TranslationLanguageSupport.AnyLanguage)

        override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation =
            error("Not used")

        override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation =
            error("Not used")

        override suspend fun translate(ready: ReadyTranslationEngineRequest, text: String): TranslationEngineExecution =
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
            artwork = TranslationEngineArtwork.Bundled(1),
            details = TranslationEngineDetails(
                description = "Test engine description",
                processingLocation = "Test processing location",
                privacyDescription = "Test privacy description",
            ),
        )
    }
}
