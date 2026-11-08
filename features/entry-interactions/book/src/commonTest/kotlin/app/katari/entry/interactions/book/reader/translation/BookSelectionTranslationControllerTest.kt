package app.katari.entry.interactions.book.reader.translation

import app.katari.entry.interactions.book.reader.language.BookSelectionLanguageSession
import app.katari.entry.viewer.settings.ResolvedViewerSetting
import app.katari.entry.viewer.settings.ViewerSettingSource
import app.katari.entry.viewer.settings.shared.StandardReaderCapabilities
import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.availability.TranslationDeviceAvailability
import app.katari.translation.api.engine.KnownTranslationEngine
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineInspection
import app.katari.translation.api.engine.TranslationEngineState
import app.katari.translation.api.host.TranslationHostActionResult
import app.katari.translation.api.host.TranslationHostActions
import app.katari.translation.api.language.TranslationDefaultTarget
import app.katari.translation.api.language.TranslationLanguageSupport
import app.katari.translation.api.language.TranslationLanguageSupportInspection
import app.katari.translation.api.model.TranslationModelDescriptor
import app.katari.translation.api.preparation.ReadyTranslation
import app.katari.translation.api.preparation.TranslationPreparation
import app.katari.translation.api.preparation.TranslationRoutePreparation
import app.katari.translation.api.provider.TranslationProviderDisclosure
import app.katari.translation.api.request.TranslationBatch
import app.katari.translation.api.request.TranslationRequest
import app.katari.translation.api.request.TranslationRouteRequest
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import app.katari.translation.api.result.TranslationBatchUpdate
import app.katari.translation.api.result.TranslationExecution
import app.katari.translation.ui.session.TranslationSessionState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import app.katari.entry.interactions.book.reader.selection.BookReaderTextSelection as NeutralBookReaderTextSelection

@OptIn(ExperimentalCoroutinesApi::class)
class BookSelectionTranslationControllerTest {
    @Test
    fun `translation waits for release and grabbing a handle clears it before the text changes`() = runTest {
        val feature = RecordingFeature()
        val controller = controller(feature, FakeHostActions())
        runCurrent()
        val selected = readerSelection("selected", isSettled = true)
        controller.submitSelection(selected)
        runCurrent()

        controller.submitSelection(selected.copy(isSettled = false))
        controller.submitSelection(readerSelection("expanded selection", isSettled = false))
        advanceTimeBy(1_000)
        runCurrent()

        feature.requests.map(TranslationRequest::text) shouldBe listOf("selected")
        controller.hostCoordinator.controller.state.value shouldBe TranslationSessionState.Hidden

        controller.submitSelection(readerSelection("expanded selection", isSettled = true))
        runCurrent()

        feature.requests.map(TranslationRequest::text) shouldBe listOf("selected", "expanded selection")
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

    private fun readerSelection(text: String, isSettled: Boolean) = NeutralBookReaderTextSelection(
        ownerIdentity = "owner",
        identity = text,
        text = text,
        languageContextText = "surrounding $text prose",
        languageTags = emptyList(),
        anchor = null,
        isSettled = isSettled,
    )

    private class RecordingFeature : TranslationFeature {
        val requests = mutableListOf<TranslationRequest>()

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            requests += request
            return TranslationPreparation.SourceUndetermined()
        }

        override suspend fun prepareRoute(route: TranslationRouteRequest): TranslationRoutePreparation =
            error("Selections translate text")

        override fun translateBatch(batch: TranslationBatch): Flow<TranslationBatchUpdate> =
            error("Selections translate text")

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
        override val recentLanguages: Preference<List<LanguageTag>> = store.getObjectFromString(
            "recent",
            emptyList(),
            { languages -> languages.joinToString(",") { it.value } },
            { raw -> raw.split(",").mapNotNull(LanguageTag::parse) },
        )
        var availability: TranslationDeviceAvailability = TranslationDeviceAvailability.Available

        override fun defaultTarget(): TranslationDefaultTarget? =
            (defaultTargetLanguage.get() as? TranslationTargetLanguageSelection.Explicit)
                ?.let { TranslationDefaultTarget(it.language, followsAppLanguage = false) }

        override fun recordRecentLanguage(language: LanguageTag) {
            recentLanguages.set(listOf(language) + recentLanguages.get().filterNot { it == language })
        }

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
