package mihon.translation.ui.session

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineAction
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.language.TranslationDefaultTarget
import mihon.translation.api.language.TranslationLanguagePair
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.session.language.TranslationLanguageStore
import mihon.translation.ui.session.language.TranslationStoredLanguages
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationSessionHostCoordinatorTest {
    @Test
    fun `ready selection changes only the staged session engine`() = runTest {
        val host = FakeHostActions()
        val feature = RecordingFeature()
        val coordinator = TranslationSessionHostCoordinator(
            feature = feature,
            hostActions = host,
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
        )
        runCurrent()
        coordinator.controller.submit(input())
        runCurrent()
        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseEngine) {}

        coordinator.selectEngine(READY_ENGINE.id)
        runCurrent()

        feature.requests.last().engine shouldBe TranslationEngineSelection.Explicit(READY_ENGINE.id)
        host.selectedEngine.get() shouldBe PROFILE_ENGINE.id
        coordinator.picker.value shouldBe null
    }

    @Test
    fun `blocked selection is rejected and leaves the chooser open`() = runTest {
        val host = FakeHostActions()
        val feature = RecordingFeature()
        val coordinator = TranslationSessionHostCoordinator(
            feature = feature,
            hostActions = host,
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
        )
        runCurrent()
        coordinator.controller.submit(input())
        runCurrent()
        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseEngine) {}
        val requestCount = feature.requests.size

        coordinator.selectEngine(BLOCKED_ENGINE.id)
        runCurrent()

        feature.requests.size shouldBe requestCount
        coordinator.picker.value shouldBe TranslationSessionPicker.Engine
        host.selectedEngine.get() shouldBe PROFILE_ENGINE.id
    }

    @Test
    fun `language selection rejects unsupported targets and accepts the provider pair`() = runTest {
        val host = FakeHostActions().apply {
            languageSupport = TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.ExactPairs(
                    setOf(TranslationLanguagePair(SOURCE, FRENCH)),
                ),
            )
        }
        val feature = RecordingFeature()
        val coordinator = TranslationSessionHostCoordinator(
            feature = feature,
            hostActions = host,
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
        )
        runCurrent()
        coordinator.controller.submit(input())
        runCurrent()
        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {}
        runCurrent()
        val requestCount = feature.requests.size

        coordinator.selectLanguage(TARGET)
        runCurrent()
        feature.requests.size shouldBe requestCount
        coordinator.picker.value shouldBe TranslationSessionPicker.TargetLanguage

        coordinator.selectLanguage(FRENCH)
        runCurrent()
        feature.requests.last().targetLanguage shouldBe
            TranslationTargetLanguageSelection.Explicit(FRENCH)
        coordinator.picker.value shouldBe null
    }

    @Test
    fun `a picked language becomes the most recent language`() = runTest {
        val host = FakeHostActions().apply { recentLanguages.set(listOf(FRENCH, GERMAN)) }
        val coordinator = TranslationSessionHostCoordinator(
            feature = RecordingFeature(),
            hostActions = host,
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
        )
        runCurrent()
        coordinator.controller.submit(input())
        runCurrent()
        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {}
        runCurrent()

        coordinator.selectLanguage(GERMAN)
        runCurrent()

        coordinator.recentLanguages.value shouldBe listOf(GERMAN, FRENCH)
    }

    @Test
    fun `a target picked for one selection is used by the next`() = runTest {
        val feature = RecordingFeature()
        val coordinator = TranslationSessionHostCoordinator(
            feature = feature,
            hostActions = FakeHostActions(),
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
        )
        runCurrent()
        coordinator.submit("First", TextLanguageResolutionContext(), anchor = null)
        runCurrent()
        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {}
        runCurrent()
        coordinator.selectLanguage(FRENCH)
        runCurrent()

        coordinator.submit("Second", TextLanguageResolutionContext(), anchor = null)
        runCurrent()

        feature.requests.last().text shouldBe "Second"
        feature.requests.last().targetLanguage shouldBe TranslationTargetLanguageSelection.Explicit(FRENCH)
    }

    @Test
    fun `target picker follows the profile default until a language is pinned and can return to it`() = runTest {
        val feature = RecordingFeature()
        val coordinator = TranslationSessionHostCoordinator(
            feature = feature,
            hostActions = FakeHostActions(),
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
        )
        runCurrent()
        coordinator.controller.submit(
            input().copy(request = input().request.copy(targetLanguage = TranslationTargetLanguageSelection.Default)),
        )
        runCurrent()
        val picker = TranslationSessionPicker.TargetLanguage
        val profileDefault = TranslationDefaultTarget(TARGET, followsAppLanguage = false)

        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {}
        runCurrent()
        coordinator.languageDefault(picker) shouldBe TranslationSessionLanguageDefault.Target(profileDefault, true)
        coordinator.selectedLanguage(picker) shouldBe null

        coordinator.selectLanguage(FRENCH)
        runCurrent()
        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {}
        runCurrent()
        coordinator.languageDefault(picker) shouldBe TranslationSessionLanguageDefault.Target(profileDefault, false)
        coordinator.selectedLanguage(picker) shouldBe FRENCH

        coordinator.selectLanguageDefault()
        runCurrent()
        feature.requests.last().targetLanguage shouldBe TranslationTargetLanguageSelection.Default
        coordinator.picker.value shouldBe null
    }

    @Test
    fun `automatic source row shows the detected and declared languages and undoes a pinned source`() = runTest {
        val feature = RecordingFeature()
        val coordinator = TranslationSessionHostCoordinator(
            feature = feature,
            hostActions = FakeHostActions(),
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
        )
        runCurrent()
        coordinator.controller.submit(
            input().copy(
                request = input().request.copy(
                    sourceLanguage = TranslationSourceLanguageSelection.Automatic,
                    languageContext = TextLanguageResolutionContext(declaredLanguages = listOf(FRENCH)),
                ),
            ),
        )
        runCurrent()
        val picker = TranslationSessionPicker.SourceLanguage

        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage) {}
        runCurrent()
        coordinator.languageDefault(picker) shouldBe TranslationSessionLanguageDefault.AutomaticSource(
            detected = SOURCE,
            declared = FRENCH,
            selected = true,
        )
        coordinator.selectedLanguage(picker) shouldBe null

        coordinator.selectLanguage(GERMAN)
        runCurrent()
        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage) {}
        runCurrent()
        coordinator.languageDefault(picker) shouldBe TranslationSessionLanguageDefault.AutomaticSource(
            detected = null,
            declared = FRENCH,
            selected = false,
        )
        coordinator.selectedLanguage(picker) shouldBe GERMAN

        coordinator.selectLanguageDefault()
        runCurrent()
        feature.requests.last().sourceLanguage shouldBe TranslationSourceLanguageSelection.Automatic
        coordinator.picker.value shouldBe null
    }

    @Test
    fun `a target the series starts keeping is announced once and can become the profile default`() = runTest {
        val host = FakeHostActions()
        val store = FakeLanguageStore()
        val feature = RecordingFeature()
        val coordinator = TranslationSessionHostCoordinator(
            feature = feature,
            hostActions = host,
            scope = backgroundScope,
            selectionSettleDelayMillis = 0,
            languageStore = store,
        )
        val announced = mutableListOf<LanguageTag>()
        backgroundScope.launch { coordinator.newlyKeptTargets.collect(announced::add) }
        runCurrent()
        coordinator.submit("Text", TextLanguageResolutionContext(), anchor = null)
        runCurrent()

        listOf(FRENCH, GERMAN).forEach { language ->
            coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {}
            runCurrent()
            coordinator.selectLanguage(language)
            runCurrent()
        }
        announced shouldBe listOf(FRENCH)

        coordinator.makeDefaultTarget(GERMAN)
        runCurrent()

        host.defaultTargetLanguage.get() shouldBe TranslationTargetLanguageSelection.Explicit(GERMAN)
        store.stored.value.target shouldBe null
        feature.requests.last().targetLanguage shouldBe TranslationTargetLanguageSelection.Default
    }

    @Test
    fun `the target picker follows the series choice when no translation is on screen`() = runTest {
        val store = FakeLanguageStore().apply { stored.value = TranslationStoredLanguages(target = FRENCH) }
        val coordinator = TranslationSessionHostCoordinator(
            feature = RecordingFeature(),
            hostActions = FakeHostActions(),
            scope = backgroundScope,
            languageStore = store,
        )
        runCurrent()
        val picker = TranslationSessionPicker.TargetLanguage

        coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {}
        runCurrent()
        coordinator.languageDefault(picker) shouldBe TranslationSessionLanguageDefault.Target(
            TranslationDefaultTarget(TARGET, followsAppLanguage = false),
            selected = false,
        )
        coordinator.selectedLanguage(picker) shouldBe FRENCH

        coordinator.selectLanguageDefault()
        runCurrent()
        store.stored.value.target shouldBe null
    }

    @Test
    fun `setup refreshes state without selecting the engine`() = runTest {
        val host = FakeHostActions()
        val coordinator = TranslationSessionHostCoordinator(
            feature = RecordingFeature(),
            hostActions = host,
            scope = backgroundScope,
        )
        runCurrent()
        val inspectionsBeforeSetup = host.inspectionCount

        coordinator.openEngineSetup(BLOCKED_ENGINE.id)
        runCurrent()

        host.openedEngine shouldBe BLOCKED_ENGINE.id
        host.selectedEngine.get() shouldBe PROFILE_ENGINE.id
        host.inspectionCount shouldBe inspectionsBeforeSetup + 1
    }

    @Test
    fun `engine inspection can leave the implicit profile selection unconfigured`() = runTest {
        val host = FakeHostActions().apply {
            inspectedSelection = null
        }
        val coordinator = TranslationSessionHostCoordinator(
            feature = RecordingFeature(),
            hostActions = host,
            scope = backgroundScope,
        )

        runCurrent()

        coordinator.profileSelectedEngine shouldBe null
        coordinator.close()
    }

    @Test
    fun `returning from either setup destination retries the session once`() = runTest {
        TranslationSetupDestination.entries.forEach { destination ->
            val host = FakeHostActions().apply {
                setupResult = TranslationHostActionResult.SetupOpened(destination)
            }
            val feature = RecordingFeature()
            val coordinator = TranslationSessionHostCoordinator(
                feature = feature,
                hostActions = host,
                scope = backgroundScope,
            )
            runCurrent()
            coordinator.controller.submit(input())
            runCurrent()
            val requestsBeforeSetup = feature.requests.size

            coordinator.openEngineSetup(BLOCKED_ENGINE.id)
            runCurrent()

            feature.requests.size shouldBe requestsBeforeSetup

            coordinator.onResume()
            runCurrent()
            feature.requests.size shouldBe requestsBeforeSetup + 1

            coordinator.onResume()
            runCurrent()
            feature.requests.size shouldBe requestsBeforeSetup + 1
            coordinator.close()
        }
    }

    private class RecordingFeature : TranslationFeature {
        val requests = mutableListOf<TranslationRequest>()

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            requests += request
            return TranslationPreparation.TargetLanguageRequired(
                sourceLanguage = (request.sourceLanguage as? TranslationSourceLanguageSelection.Explicit)?.language
                    ?: SOURCE,
                reason = TranslationTargetChoiceReason.NoDefaultTarget,
            )
        }

        override suspend fun translate(ready: ReadyTranslation): TranslationExecution = error("Not used")
    }

    private class FakeHostActions : TranslationHostActions {
        override val knownEngines = listOf(PROFILE_ENGINE, READY_ENGINE, BLOCKED_ENGINE)
        override val selectedEngine = InMemoryPreference("engine", null, PROFILE_ENGINE.id)
        override val defaultTargetLanguage: Preference<TranslationTargetLanguageSelection> = InMemoryPreference(
            "target",
            null,
            TranslationTargetLanguageSelection.Explicit(TARGET),
        )
        override val recentLanguages: Preference<List<LanguageTag>> =
            InMemoryPreference("recent", null, emptyList())
        var inspectionCount = 0
        var inspectedSelection: TranslationEngineId? = PROFILE_ENGINE.id
        var openedEngine: TranslationEngineId? = null
        var setupResult: TranslationHostActionResult = TranslationHostActionResult.Completed
        var languageSupport: TranslationLanguageSupportInspection =
            TranslationLanguageSupportInspection.Available(TranslationLanguageSupport.AnyLanguage)

        override fun defaultTarget(): TranslationDefaultTarget? =
            (defaultTargetLanguage.get() as? TranslationTargetLanguageSelection.Explicit)
                ?.let { TranslationDefaultTarget(it.language, followsAppLanguage = false) }

        override fun recordRecentLanguage(language: LanguageTag) {
            recentLanguages.set(listOf(language) + recentLanguages.get().filterNot { it == language })
        }

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngines(): TranslationEngineInspection {
            inspectionCount += 1
            val engines = knownEngines.map { engine ->
                TranslationEngineState(
                    engine = engine,
                    presentation = null,
                    status = if (engine == BLOCKED_ENGINE) {
                        TranslationEngineStatus.NotInstalled
                    } else {
                        TranslationEngineStatus.Ready
                    },
                    action = if (engine == BLOCKED_ENGINE) TranslationEngineAction.Install else null,
                )
            }
            return TranslationEngineInspection(engines, inspectedSelection)
        }

        override suspend fun inspectLanguageSupport(engine: TranslationEngineId) =
            languageSupport

        override suspend fun acknowledgeProviderDisclosure(
            engine: TranslationEngineId,
            disclosure: TranslationProviderDisclosure,
        ) = TranslationHostActionResult.Completed

        override suspend fun downloadModels(
            engine: TranslationEngineId,
            models: List<TranslationModelDescriptor>,
            allowMeteredNetwork: Boolean,
        ) = TranslationHostActionResult.Completed

        override fun supportsSetup(engine: TranslationEngineId) = true

        override suspend fun openSetup(engine: TranslationEngineId): TranslationHostActionResult {
            openedEngine = engine
            return setupResult
        }

        override fun setSelectedEngine(engine: TranslationEngineId) {
            selectedEngine.set(engine)
        }

        override fun setDefaultTargetLanguage(language: LanguageTag?) {
            defaultTargetLanguage.set(
                language?.let(TranslationTargetLanguageSelection::Explicit)
                    ?: TranslationTargetLanguageSelection.Default,
            )
        }
    }

    private class FakeLanguageStore : TranslationLanguageStore {
        val stored = MutableStateFlow(TranslationStoredLanguages())
        override val languages: Flow<TranslationStoredLanguages> = stored

        override suspend fun setSourceLanguage(language: LanguageTag?) {
            stored.update { it.copy(source = language) }
        }

        override suspend fun setTargetLanguage(language: LanguageTag?) {
            stored.update { it.copy(target = language) }
        }
    }

    private companion object {
        val SOURCE = LanguageTag.require("en")
        val TARGET = LanguageTag.require("pl")
        val FRENCH = LanguageTag.require("fr")
        val GERMAN = LanguageTag.require("de")
        val PROFILE_ENGINE = engine("profile")
        val READY_ENGINE = engine("ready")
        val BLOCKED_ENGINE = engine("blocked")

        fun engine(id: String) = KnownTranslationEngine(
            id = TranslationEngineId(id),
            providerId = TranslationProviderId(id),
            providerName = "$id provider",
            engineName = "$id engine",
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = TranslationEngineArtwork.Bundled(1),
            details = TranslationEngineDetails(
                description = "$id description",
                processingLocation = "$id processing",
                privacyDescription = "$id privacy",
            ),
        )

        fun input() = TranslationSessionInput(
            request = TranslationRequest(
                text = "Text",
                sourceLanguage = TranslationSourceLanguageSelection.Explicit(SOURCE),
                targetLanguage = TranslationTargetLanguageSelection.Explicit(TARGET),
            ),
        )
    }
}
