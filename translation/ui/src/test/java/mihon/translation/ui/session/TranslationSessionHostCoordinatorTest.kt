package mihon.translation.ui.session

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ReadyTranslation
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineAction
import mihon.translation.api.TranslationEngineArtwork
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineDetails
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationEngineState
import mihon.translation.api.TranslationEngineStatus
import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationSetupDestination
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetChoiceReason
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference

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
                sourceLanguage = SOURCE,
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
        var inspectionCount = 0
        var openedEngine: TranslationEngineId? = null
        var setupResult: TranslationHostActionResult = TranslationHostActionResult.Completed

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngineStates(): List<TranslationEngineState> {
            inspectionCount += 1
            return knownEngines.map { engine ->
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
        }

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

        override fun setDefaultTargetLanguage(language: TranslationLanguageTag?) = Unit
    }

    private companion object {
        val SOURCE = TranslationLanguageTag.require("en")
        val TARGET = TranslationLanguageTag.require("pl")
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
