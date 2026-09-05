package mihon.entry.interactions.book.reader.translation

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.reader.language.BookSelectionLanguageSession
import mihon.entry.viewer.settings.ResolvedViewerSetting
import mihon.entry.viewer.settings.ViewerSettingSource
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionState
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import mihon.entry.interactions.book.reader.selection.BookReaderTextSelection as NeutralBookReaderTextSelection

@OptIn(ExperimentalCoroutinesApi::class)
class BookSelectionTranslationControllerTest {
    @Test
    fun `holding a selection still does not prepare translation until release`() = runTest {
        val feature = RecordingFeature()
        val controller = controller(feature, FakeHostActions())
        runCurrent()
        val held = readerSelection("selected", isSettled = false)

        controller.submitSelection(held)
        advanceTimeBy(1_000)
        runCurrent()

        feature.requests shouldBe emptyList()
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden

        controller.submitSelection(held.copy(isSettled = true))
        advanceTimeBy(250)
        runCurrent()

        feature.requests.map(TranslationRequest::text) shouldBe listOf("selected")
        controller.close()
    }

    @Test
    fun `grabbing a handle cancels pending translation even before selected text changes`() = runTest {
        val feature = RecordingFeature()
        val controller = controller(feature, FakeHostActions())
        runCurrent()
        val selected = readerSelection("selected", isSettled = true)
        controller.submitSelection(selected)
        advanceTimeBy(100)

        controller.submitSelection(selected.copy(isSettled = false))
        controller.submitSelection(readerSelection("expanded selection", isSettled = false))
        advanceTimeBy(1_000)
        runCurrent()

        feature.requests shouldBe emptyList()
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden

        controller.submitSelection(readerSelection("expanded selection", isSettled = true))
        advanceTimeBy(250)
        runCurrent()

        feature.requests.map(TranslationRequest::text) shouldBe listOf("expanded selection")
        controller.close()
    }

    @Test
    fun `settling prepares only the latest selected text`() = runTest {
        val feature = RecordingFeature()
        val host = FakeHostActions()
        val automaticSelectionSetting = automaticSelectionSetting(enabled = true)
        val controller = controller(feature, host, automaticSelectionSetting)
        runCurrent()

        controller.submitSelection(selection("first", 1))
        advanceTimeBy(100)
        controller.submitSelection(selection("second", 2))
        advanceTimeBy(250)
        runCurrent()

        feature.requests.map(TranslationRequest::text) shouldBe listOf("second")
        feature.requests.single().engine shouldBe TranslationEngineSelection.ProfileDefault
        feature.requests.single().languageContext.surroundingText shouldBe "surrounding second prose"
        feature.requests.single().languageContext.declaredLanguages shouldBe listOf(LanguageTag.require("en"))
        controller.close()
    }

    @Test
    fun `availability loss preserves preference and clears the session`() = runTest {
        val feature = RecordingFeature()
        val host = FakeHostActions()
        val automaticSelectionSetting = automaticSelectionSetting(enabled = true)
        val controller = controller(feature, host, automaticSelectionSetting)
        runCurrent()
        controller.submitSelection(selection("selected", 1))
        advanceTimeBy(250)
        runCurrent()

        host.availability = TranslationDeviceAvailability.TranslationServiceMissing
        controller.onResume()
        runCurrent()

        automaticSelectionSetting.value.effectiveValue shouldBe true
        controller.effectiveEnabled.value shouldBe false
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden
        controller.submitSelection(selection("ignored", 2))
        advanceTimeBy(250)
        runCurrent()
        feature.requests.map(TranslationRequest::text) shouldBe listOf("selected")
        controller.close()
    }

    @Test
    fun `entry setting change updates effective translation behavior`() = runTest {
        val setting = automaticSelectionSetting(enabled = true)
        val controller = controller(RecordingFeature(), FakeHostActions(), setting)
        runCurrent()
        controller.submitSelection(selection("selected", 1))

        setting.value = setting.value.copy(effectiveValue = false, entryOverride = false)
        runCurrent()

        controller.effectiveEnabled.value shouldBe false
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden
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

    @Test
    fun `reader tap dismisses an active translation before reader interaction`() = runTest {
        val feature = RecordingFeature()
        val controller = controller(feature, FakeHostActions())
        runCurrent()
        val selection = selection("selected", 1)
        controller.submitSelection(selection)

        controller.dismissTranslationOnReaderTap() shouldBe true
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden

        controller.submitSelection(selection)
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden
        controller.dismissTranslationOnReaderTap() shouldBe false
        controller.close()
    }

    private fun TestScope.controller(
        feature: RecordingFeature,
        host: FakeHostActions,
        automaticSelectionSetting: MutableStateFlow<ResolvedViewerSetting<Boolean>> =
            automaticSelectionSetting(enabled = true),
    ) = BookSelectionTranslationController(
        feature = feature,
        hostActions = host,
        automaticSelectionSetting = automaticSelectionSetting,
        languageSession = BookSelectionLanguageSession(listOf("en")),
        scope = backgroundScope,
        initialCapabilities = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        ),
    )

    private fun automaticSelectionSetting(enabled: Boolean) = MutableStateFlow(
        ResolvedViewerSetting(
            effectiveValue = enabled,
            source = ViewerSettingSource.ENTRY,
            processorDefault = false,
            profileValue = false,
            entryOverride = enabled,
        ),
    )

    private fun selection(text: String, generation: Int) = BookReaderTextSelection(
        ownerIdentity = "owner",
        identity = "selection-$generation",
        text = text,
        languageContextText = "surrounding $text prose",
        anchor = TranslationSelectionAnchor(10f, 20f, 30f, 40f),
    )

    private fun readerSelection(text: String, isSettled: Boolean) = NeutralBookReaderTextSelection(
        ownerIdentity = "owner",
        identity = text,
        text = text,
        languageContextText = "surrounding $text prose",
        anchor = null,
        isSettled = isSettled,
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
            Preference<TranslationTargetLanguageSelection> =
            store.getObjectFromString(
                "target",
                TranslationTargetLanguageSelection.Default,
                { "default" },
                { TranslationTargetLanguageSelection.Default },
            )
        var availability: TranslationDeviceAvailability = TranslationDeviceAvailability.Available

        override suspend fun deviceAvailability() = availability

        override suspend fun inspectEngines() = TranslationEngineInspection(
            engines = emptyList<TranslationEngineState>(),
            selectedEngine = selectedEngine.get(),
        )

        override suspend fun inspectLanguageSupport(engine: TranslationEngineId) =
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.AnyLanguage,
            )

        override suspend fun acknowledgeProviderDisclosure(
            engine: TranslationEngineId,
            disclosure: TranslationProviderDisclosure,
        ) = TranslationHostActionResult.Completed

        override suspend fun downloadModels(
            engine: TranslationEngineId,
            models: List<TranslationModelDescriptor>,
            allowMeteredNetwork: Boolean,
        ) = TranslationHostActionResult.ModelsReady

        override fun supportsSetup(engine: TranslationEngineId) = false

        override suspend fun openSetup(engine: TranslationEngineId) =
            TranslationHostActionResult.SetupUnsupported

        override fun setSelectedEngine(engine: TranslationEngineId) {
            selectedEngine.set(engine)
        }

        override fun setDefaultTargetLanguage(language: LanguageTag?) = Unit
    }
}
