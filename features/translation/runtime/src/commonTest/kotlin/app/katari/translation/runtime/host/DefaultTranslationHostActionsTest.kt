package app.katari.translation.runtime.host

import app.katari.translation.api.engine.KnownTranslationEngine
import app.katari.translation.api.engine.TranslationEngineArtwork
import app.katari.translation.api.engine.TranslationEngineBuildAvailability
import app.katari.translation.api.engine.TranslationEngineDetails
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineInspection
import app.katari.translation.api.engine.TranslationEngineStatus
import app.katari.translation.api.engine.TranslationProviderId
import app.katari.translation.api.language.TranslationLanguageSupport
import app.katari.translation.api.language.TranslationLanguageSupportInspection
import app.katari.translation.api.preparation.TranslationUnavailableReason
import app.katari.translation.api.provider.TranslationInvocationPolicy
import app.katari.translation.api.provider.TranslationProviderPresentation
import app.katari.translation.api.request.ResolvedTranslationRoute
import app.katari.translation.runtime.preference.ProfileTranslationPreferences
import app.katari.translation.runtime.registry.DefaultTranslationEngineRegistry
import app.katari.translation.runtime.selection.ProfileTranslationEngineResolver
import app.katari.translation.spi.contribution.TranslationEngineContribution
import app.katari.translation.spi.engine.ReadyTranslationEngineRequest
import app.katari.translation.spi.engine.TranslationEngine
import app.katari.translation.spi.engine.TranslationEngineDeviceAvailability
import app.katari.translation.spi.engine.TranslationEngineExecution
import app.katari.translation.spi.engine.TranslationEnginePreparation
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
