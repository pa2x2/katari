package eu.kanade.presentation.more.settings.screen.translation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationSystemSetupReason
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.runtime.ProfileTranslationPreferences
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.spi.TranslationEngineSetup
import mihon.translation.spi.TranslationEngineSetupRegistry
import mihon.translation.ui.session.TranslationSessionState
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class TranslationSettingsScreenModelTest {

    @Test
    fun `playground checks a real explicit pair before translation and engine experiments stay transient`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), ANDROID_ENGINE).apply {
            targetLanguage.set(TranslationTargetLanguageSelection.Explicit(ENGLISH))
        }
        val feature = SetupRequiredFeature()
        val model = TranslationSettingsScreenModel(
            feature = feature,
            preferences = preferences,
            knownEngineCatalog = object : KnownTranslationEngineCatalog {
                override val knownEngines = listOf(knownEngine(ANDROID_ENGINE))
            },
            setupRegistry = object : TranslationEngineSetupRegistry {
                override fun find(engine: TranslationEngineId): TranslationEngineSetup? = null
            },
        )

        try {
            advanceUntilIdle()

            feature.lastRequest shouldBe TranslationRequest(
                text = "Bonjour tout le monde",
                sourceLanguage = TranslationSourceLanguageSelection.Explicit(FRENCH),
                targetLanguage = TranslationTargetLanguageSelection.Explicit(ENGLISH),
                engine = TranslationEngineSelection.Explicit(ANDROID_ENGINE),
            )
            model.controller.state.value
                .shouldBeInstanceOf<TranslationSessionState.PreparationRequired>()

            model.swapLanguages()
            advanceUntilIdle()
            feature.lastRequest?.sourceLanguage shouldBe
                TranslationSourceLanguageSelection.Explicit(ENGLISH)
            feature.lastRequest?.targetLanguage shouldBe
                TranslationTargetLanguageSelection.Explicit(FRENCH)

            model.setEngine(SECOND_ENGINE)
            advanceUntilIdle()
            preferences.engine.get() shouldBe ANDROID_ENGINE

            model.usePlaygroundEngineAsDefault()
            preferences.engine.get() shouldBe SECOND_ENGINE
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    private class SetupRequiredFeature : TranslationFeature {
        var lastRequest: TranslationRequest? = null

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            lastRequest = request
            return TranslationPreparation.SystemSetupRequired(
                engine = (request.engine as TranslationEngineSelection.Explicit).engine,
                presentation = PRESENTATION,
                reason = TranslationSystemSetupReason.LanguageModelsRequired,
            )
        }

        override suspend fun translate(
            ready: mihon.translation.api.ReadyTranslation,
        ): TranslationExecution = error("Playground must not execute before the user action")
    }

    private companion object {
        val ANDROID_ENGINE = TranslationEngineId("android-system")
        val SECOND_ENGINE = TranslationEngineId("second")
        val ENGLISH = TranslationLanguageTag.require("en")
        val FRENCH = TranslationLanguageTag.require("fr")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = TranslationProviderId("android"),
            providerName = "Android",
            engineName = "System on-device translation",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )

        fun knownEngine(id: TranslationEngineId) = KnownTranslationEngine(
            id = id,
            providerId = PRESENTATION.providerId,
            providerName = PRESENTATION.providerName,
            engineName = PRESENTATION.engineName,
            buildAvailability = TranslationEngineBuildAvailability.Included,
        )
    }
}
