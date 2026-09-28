package mihon.translation.ui.session

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
        var setupResult: TranslationHostActionResult = TranslationHostActionResult.Completed

        override fun defaultTarget(): TranslationDefaultTarget? =
            (defaultTargetLanguage.get() as? TranslationTargetLanguageSelection.Explicit)
                ?.let { TranslationDefaultTarget(it.language, followsAppLanguage = false) }

        override fun recordRecentLanguage(language: LanguageTag) {
            recentLanguages.set(listOf(language) + recentLanguages.get().filterNot { it == language })
        }

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngines(): TranslationEngineInspection {
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
            return TranslationEngineInspection(engines, PROFILE_ENGINE.id)
        }

        override suspend fun inspectLanguageSupport(engine: TranslationEngineId) =
            TranslationLanguageSupportInspection.Available(TranslationLanguageSupport.AnyLanguage)

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

        override suspend fun openSetup(engine: TranslationEngineId) = setupResult

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

    private companion object {
        val SOURCE = LanguageTag.require("en")
        val TARGET = LanguageTag.require("pl")
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
