package mihon.translation.runtime

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineAction
import mihon.translation.api.TranslationEngineArtwork
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineDetails
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineStatus
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguagePair
import mihon.translation.api.TranslationLanguageSupport
import mihon.translation.api.TranslationLanguageSupportInspection
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationSetupDestination
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.runtime.selection.ProfileTranslationEngineResolver
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineContribution
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import mihon.translation.spi.TranslationEngineSetup
import mihon.translation.spi.TranslationSetupResult
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.coroutines.suspendCoroutine

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
        val emissions = mutableListOf<mihon.translation.api.TranslationEngineInspection>()
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
        val emissions = mutableListOf<mihon.translation.api.TranslationEngineInspection>()
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
    fun `engine inspection exposes typed readiness and generic recovery actions`() = runTest {
        val notInstalled = actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.NotInstalled),
            setups = listOf(FakeSetup(supportsSetup = true)),
        ).inspectEngines().engines.single()
        notInstalled.status shouldBe TranslationEngineStatus.NotInstalled
        notInstalled.action shouldBe TranslationEngineAction.Install

        val configurationRequired = actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.ConfigurationRequired("Enable local API")),
            setups = listOf(FakeSetup(supportsSetup = true)),
        ).inspectEngines().engines.single()
        configurationRequired.status shouldBe
            TranslationEngineStatus.ConfigurationRequired("Enable local API")
        configurationRequired.action shouldBe TranslationEngineAction.Configure

        val ready = actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.Available),
            setups = listOf(FakeSetup(supportsSetup = true)),
        ).inspectEngines().engines.single()
        ready.status shouldBe TranslationEngineStatus.Ready
        ready.action shouldBe TranslationEngineAction.Setup
    }

    @Test
    fun `device inspection maps request-independent engine states`() = runTest {
        val cases = listOf(
            TranslationEngineDeviceAvailability.Available to TranslationDeviceAvailability.Available,
            TranslationEngineDeviceAvailability.NotInstalled to
                TranslationDeviceAvailability.SelectedEngineUnavailable(
                    ENGINE_ID,
                    "Provider application is not installed",
                ),
            TranslationEngineDeviceAvailability.ConfigurationRequired("enable API") to
                TranslationDeviceAvailability.SelectedEngineUnavailable(ENGINE_ID, "enable API"),
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
    fun `language support remains owned by the explicitly requested engine`() = runTest {
        val support = TranslationLanguageSupport.ExactPairs(
            setOf(TranslationLanguagePair(ENGLISH, POLISH)),
        )
        actions(
            engine = FakeEngine(
                availability = TranslationEngineDeviceAvailability.Available,
                languageSupport = TranslationLanguageSupportInspection.Available(support),
            ),
        ).inspectLanguageSupport(ENGINE_ID) shouldBe
            TranslationLanguageSupportInspection.Available(support)

        actions(engine = null).inspectLanguageSupport(ENGINE_ID) shouldBe
            TranslationLanguageSupportInspection.Unavailable(
                "Translation engine is not available",
            )
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

    @Test
    fun `missing selected engines remain explicit and never fall back`() = runTest {
        actions(engine = null, known = listOf(KNOWN_ENGINE)).deviceAvailability() shouldBe
            TranslationDeviceAvailability.SelectedEngineUnavailable(ENGINE_ID)
        actions(engine = null, known = emptyList()).deviceAvailability() shouldBe
            TranslationDeviceAvailability.SelectedEngineMissing(ENGINE_ID)
    }

    @Test
    fun `system setup shortcut is exposed only when the selected engine declares support`() {
        actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.Available),
            setups = listOf(FakeSetup(supportsSetup = true)),
        ).supportsSetup(ENGINE_ID) shouldBe true
        actions(
            engine = FakeEngine(TranslationEngineDeviceAvailability.Available),
            setups = listOf(FakeSetup(supportsSetup = false)),
        ).supportsSetup(ENGINE_ID) shouldBe false
        actions(engine = null).supportsSetup(ENGINE_ID) shouldBe false
    }

    @Test
    fun `setup destination is preserved for host presentation`() = runTest {
        TranslationSetupDestination.entries.forEach { destination ->
            val result = actions(
                engine = FakeEngine(TranslationEngineDeviceAvailability.Available),
                setups = listOf(
                    FakeSetup(
                        supportsSetup = true,
                        destination = destination,
                    ),
                ),
            ).openSetup(ENGINE_ID)

            result shouldBe TranslationHostActionResult.SetupOpened(destination)
        }
    }

    private fun actions(
        engine: FakeEngine?,
        known: List<KnownTranslationEngine> = listOf(KNOWN_ENGINE),
        registeredEngines: List<FakeEngine> = listOfNotNull(engine),
        setups: List<TranslationEngineSetup> = emptyList(),
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
                    setup = setups.firstOrNull { it.engine == catalogEntry.id },
                )
            },
        )
        return DefaultTranslationHostActions(
            preferences = preferences,
            engineRegistry = registry,
            knownEngineCatalog = registry,
            setupRegistry = registry,
            profileEngineResolver = ProfileTranslationEngineResolver(preferences, registry),
            inspectionDispatcher = inspectionDispatcher ?: kotlinx.coroutines.Dispatchers.IO,
            inspectionTimeoutMillis = inspectionTimeoutMillis,
        )
    }

    private class FakeSetup(
        override val supportsSetup: Boolean,
        private val destination: TranslationSetupDestination = TranslationSetupDestination.External,
    ) : TranslationEngineSetup {
        override val engine = ENGINE_ID

        override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) = Unit

        override suspend fun openSetup() = TranslationSetupResult.Opened(destination)

        override suspend fun downloadModels(
            models: Set<TranslationModelId>,
            allowMeteredNetwork: Boolean,
        ) = TranslationModelOperationResult.Completed
    }

    private class FakeEngine(
        private val inspectAvailability: suspend () -> TranslationEngineDeviceAvailability,
        private val languageSupport: TranslationLanguageSupportInspection =
            TranslationLanguageSupportInspection.Available(TranslationLanguageSupport.AnyLanguage),
        override val catalogEntry: KnownTranslationEngine = KNOWN_ENGINE,
    ) : TranslationEngine {
        constructor(
            availability: TranslationEngineDeviceAvailability,
            languageSupport: TranslationLanguageSupportInspection =
                TranslationLanguageSupportInspection.Available(TranslationLanguageSupport.AnyLanguage),
            catalogEntry: KnownTranslationEngine = KNOWN_ENGINE,
        ) : this({ availability }, languageSupport, catalogEntry)

        override val presentation = TranslationProviderPresentation(
            providerId = catalogEntry.providerId,
            providerName = catalogEntry.providerName,
            engineName = catalogEntry.engineName,
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        override val maximumInputCodePoints: Int? = null
        var inspectionCount = 0
        var preparationCount = 0

        override suspend fun inspectDevice(): TranslationEngineDeviceAvailability {
            inspectionCount++
            return inspectAvailability()
        }

        override suspend fun inspectLanguageSupport() = languageSupport

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
        val SECOND_ENGINE_ID = TranslationEngineId("second-engine")
        val ENGLISH = TranslationLanguageTag.require("en")
        val POLISH = TranslationLanguageTag.require("pl")
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
