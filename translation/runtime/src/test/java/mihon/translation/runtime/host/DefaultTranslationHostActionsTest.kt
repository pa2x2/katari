package mihon.translation.runtime.host

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.translation.api.availability.TranslationDeviceAvailability
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
import mihon.translation.api.request.ResolvedTranslationRequest
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
    fun `engine inspection publishes provider results independently`() = runTest {
        val preferredAvailability = CompletableDeferred<TranslationEngineDeviceAvailability>()
        val preferredEngine = FakeEngine(
            inspectAvailability = { preferredAvailability.await() },
        )
        val secondEngine = FakeEngine(
            availability = TranslationEngineDeviceAvailability.Available,
            catalogEntry = SECOND_KNOWN_ENGINE,
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val emissions = mutableListOf<TranslationEngineInspection>()
        val collection = backgroundScope.launch(dispatcher) {
            actions(
                engine = null,
                known = listOf(KNOWN_ENGINE, SECOND_KNOWN_ENGINE),
                registeredEngines = listOf(preferredEngine, secondEngine),
                explicitSelection = false,
                inspectionDispatcher = dispatcher,
            ).inspectEngineStates().toList(emissions)
        }

        runCurrent()

        emissions.first().engines.associate { it.engine.id to it.status } shouldBe mapOf(
            ENGINE_ID to TranslationEngineStatus.Checking,
            SECOND_ENGINE_ID to TranslationEngineStatus.Checking,
        )
        emissions.last().engines.associate { it.engine.id to it.status } shouldBe mapOf(
            ENGINE_ID to TranslationEngineStatus.Checking,
            SECOND_ENGINE_ID to TranslationEngineStatus.Ready,
        )
        emissions.last().selectionResolved shouldBe false
        collection.isActive shouldBe true

        preferredAvailability.complete(TranslationEngineDeviceAvailability.Available)
        runCurrent()

        emissions.last().engines.associate { it.engine.id to it.status } shouldBe mapOf(
            ENGINE_ID to TranslationEngineStatus.Ready,
            SECOND_ENGINE_ID to TranslationEngineStatus.Ready,
        )
        emissions.last().selectedEngine shouldBe ENGINE_ID
        emissions.last().selectionResolved shouldBe true
        collection.isCompleted shouldBe true
    }

    @Test
    fun `engine inspection timeout resolves loading when provider does not return`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val emissions = mutableListOf<TranslationEngineInspection>()
        val collection = backgroundScope.launch(dispatcher) {
            actions(
                engine = FakeEngine(
                    inspectAvailability = { suspendCoroutine { } },
                ),
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

    @Test
    fun `implicit default is selected only while it is available`() = runTest {
        val available = actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.Available),
            explicitSelection = false,
        )
        available.inspectEngines().selectedEngine shouldBe ENGINE_ID
        available.deviceAvailability() shouldBe TranslationDeviceAvailability.Available

        val unavailable = actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.ServiceMissing),
            explicitSelection = false,
        )
        unavailable.inspectEngines().selectedEngine shouldBe null
        unavailable.deviceAvailability() shouldBe TranslationDeviceAvailability.EngineNotConfigured

        val explicitlySelected = actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.ServiceMissing),
        )
        explicitlySelected.inspectEngines().selectedEngine shouldBe ENGINE_ID
        explicitlySelected.deviceAvailability() shouldBe TranslationDeviceAvailability.TranslationServiceMissing
    }

    private fun actions(
        engine: FakeEngine?,
        known: List<KnownTranslationEngine> = listOf(KNOWN_ENGINE),
        registeredEngines: List<FakeEngine> = listOfNotNull(engine),
        explicitSelection: Boolean = true,
        inspectionDispatcher: CoroutineDispatcher? = null,
        inspectionTimeoutMillis: Long = 10_000,
    ): DefaultTranslationHostActions {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), ENGINE_ID)
        if (explicitSelection) preferences.engine.set(ENGINE_ID)
        val registry = DefaultTranslationEngineRegistry(
            contributions = known.map { catalogEntry ->
                TranslationEngineContribution(
                    catalogEntry = catalogEntry,
                    engine = registeredEngines.firstOrNull { it.catalogEntry.id == catalogEntry.id },
                )
            },
        )
        return DefaultTranslationHostActions(
            preferences = preferences,
            engineRegistry = registry,
            knownEngineCatalog = registry,
            setupRegistry = registry,
            profileEngineResolver = ProfileTranslationEngineResolver(preferences, registry),
            defaultTargetResolver = { null },
            inspectionDispatcher = inspectionDispatcher ?: Dispatchers.IO,
            inspectionTimeoutMillis = inspectionTimeoutMillis,
        )
    }

    private class FakeEngine(
        private val inspectAvailability: suspend () -> TranslationEngineDeviceAvailability,
        override val catalogEntry: KnownTranslationEngine = KNOWN_ENGINE,
    ) : TranslationEngine {
        constructor(
            availability: TranslationEngineDeviceAvailability,
            catalogEntry: KnownTranslationEngine = KNOWN_ENGINE,
        ) : this({ availability }, catalogEntry)

        override val presentation = TranslationProviderPresentation(
            providerId = catalogEntry.providerId,
            providerName = catalogEntry.providerName,
            engineName = catalogEntry.engineName,
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        override val maximumInputCodePoints: Int? = null

        override suspend fun inspectDevice() = inspectAvailability()

        override suspend fun inspectLanguageSupport() =
            TranslationLanguageSupportInspection.Available(TranslationLanguageSupport.AnyLanguage)

        override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation =
            error("Not used")

        override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation =
            error("Not used")

        override suspend fun translate(ready: ReadyTranslationEngineRequest): TranslationEngineExecution =
            error("Not used")
    }

    private companion object {
        val ENGINE_ID = TranslationEngineId("test-engine")
        val SECOND_ENGINE_ID = TranslationEngineId("second-engine")
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
        val SECOND_KNOWN_ENGINE = KNOWN_ENGINE.copy(
            id = SECOND_ENGINE_ID,
            engineName = "Second engine",
        )
    }
}
