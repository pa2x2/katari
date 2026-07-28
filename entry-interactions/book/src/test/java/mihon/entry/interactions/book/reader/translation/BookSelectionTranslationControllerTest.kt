package mihon.entry.interactions.book

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.viewer.settings.StandardReaderCapabilities
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ReadyTranslation
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionState
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class BookSelectionTranslationControllerTest {
    @Test
    fun `settling prepares only the latest selected text`() = runTest {
        val feature = RecordingFeature()
        val host = FakeHostActions()
        val controller = controller(feature, host)
        runCurrent()

        controller.submitSelection(selection("first", 1))
        advanceTimeBy(100)
        controller.submitSelection(selection("second", 2))
        advanceTimeBy(250)
        runCurrent()

        feature.requests.map(TranslationRequest::text) shouldBe listOf("second")
        controller.close()
    }

    @Test
    fun `availability loss preserves preference and clears the session`() = runTest {
        val feature = RecordingFeature()
        val host = FakeHostActions()
        val controller = controller(feature, host)
        runCurrent()
        controller.submitSelection(selection("selected", 1))
        advanceTimeBy(250)
        runCurrent()

        host.availability = TranslationDeviceAvailability.TranslationServiceMissing
        controller.onResume()
        runCurrent()

        host.automaticSelectionEnabled.get() shouldBe true
        controller.effectiveEnabled.value shouldBe false
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden
        controller.submitSelection(selection("ignored", 2))
        advanceTimeBy(250)
        runCurrent()
        feature.requests.map(TranslationRequest::text) shouldBe listOf("selected")
        controller.close()
    }

    @Test
    fun `anchor changes move the session without preparing unchanged text`() = runTest {
        val feature = RecordingFeature()
        val controller = controller(feature, FakeHostActions())
        runCurrent()
        val first = selection("same", 1)
        controller.submitSelection(first)
        advanceTimeBy(250)
        runCurrent()

        val moved = first.copy(anchor = TranslationSelectionAnchor(40f, 50f, 60f, 70f))
        controller.submitSelection(moved)
        runCurrent()

        feature.requests.size shouldBe 1
        val state = controller.hostCoordinator.controller.state.value as TranslationSessionState.Active
        state.input.anchor shouldBe moved.anchor
        controller.close()
    }

    private fun kotlinx.coroutines.test.TestScope.controller(
        feature: RecordingFeature,
        host: FakeHostActions,
    ) = BookSelectionTranslationController(
        feature = feature,
        hostActions = host,
        automaticSelectionEnabled = host.automaticSelectionEnabled,
        scope = backgroundScope,
        initialCapabilities = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        ),
    )

    private fun selection(text: String, generation: Int) = BookReaderTextSelection(
        ownerIdentity = "owner",
        identity = "selection-$generation",
        text = text,
        anchor = TranslationSelectionAnchor(10f, 20f, 30f, 40f),
    )

    private class RecordingFeature : TranslationFeature {
        val requests = mutableListOf<TranslationRequest>()

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            requests += request
            return TranslationPreparation.SourceUndetermined()
        }

        override suspend fun translate(ready: ReadyTranslation): TranslationExecution = error("Not ready")
    }

    private class FakeHostActions : TranslationHostActions {
        private val store = InMemoryPreferenceStore()
        override val knownEngines: List<KnownTranslationEngine> = emptyList()
        override val selectedEngine = store.getObjectFromString(
            "engine",
            TranslationEngineId("test"),
            TranslationEngineId::value,
            ::TranslationEngineId,
        )
        override val defaultTargetLanguage:
            tachiyomi.core.common.preference.Preference<TranslationTargetLanguageSelection> =
            store.getObjectFromString(
                "target",
                TranslationTargetLanguageSelection.Default,
                { "default" },
                { TranslationTargetLanguageSelection.Default },
            )
        val automaticSelectionEnabled = store.getBoolean("automatic", true)
        var availability: TranslationDeviceAvailability = TranslationDeviceAvailability.Available

        override suspend fun deviceAvailability() = availability

        override suspend fun acknowledgeProviderDisclosure(
            engine: TranslationEngineId,
            disclosure: TranslationProviderDisclosure,
        ) = TranslationHostActionResult.Completed

        override suspend fun downloadModels(
            engine: TranslationEngineId,
            models: List<TranslationModelDescriptor>,
            allowMeteredNetwork: Boolean,
        ) = TranslationHostActionResult.ModelsReady

        override fun supportsSystemSetup(engine: TranslationEngineId) = false

        override suspend fun openSystemSetup(engine: TranslationEngineId) =
            TranslationHostActionResult.SetupUnsupported

        override fun setSelectedEngine(engine: TranslationEngineId) {
            selectedEngine.set(engine)
        }

        override fun setDefaultTargetLanguage(language: TranslationLanguageTag?) = Unit
    }
}
