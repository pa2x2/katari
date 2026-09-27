package mihon.entry.interactions.manga.reader.text.translation

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.model.TranslationModelId
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderPresentation
import org.junit.jupiter.api.Test

class MangaPageTranslationIssueTest {
    @Test
    fun `language problems name the languages and the engine that cannot translate them`() {
        TranslationPreparation.TargetLanguageRequired(ENGLISH, TranslationTargetChoiceReason.SourceEqualsTarget)
            .pageTranslationIssue(ENGINE_NAME) shouldBe MangaPageTranslationIssue.SameLanguage(ENGLISH)
        TranslationPreparation.Unavailable(TranslationUnavailableReason.UnsupportedLanguagePair(JAPANESE, KOREAN))
            .pageTranslationIssue(ENGINE_NAME) shouldBe
            MangaPageTranslationIssue.UnsupportedPair(ENGINE_NAME, JAPANESE, KOREAN)
    }

    @Test
    fun `engine problems carry what their fix needs`() {
        TranslationPreparation.ModelDownloadRequired(ENGINE, PRESENTATION, listOf(MODEL))
            .pageTranslationIssue(ENGINE_NAME) shouldBe
            MangaPageTranslationIssue.LanguageDataRequired(ENGINE, PRESENTATION.engineName, listOf(MODEL))
        TranslationPreparation.EngineChoiceRequired(TranslationEngineChoiceReason.NoEngineConfigured, emptyList())
            .pageTranslationIssue(ENGINE_NAME) shouldBe MangaPageTranslationIssue.EngineChoiceRequired
        TranslationPreparation.Unavailable(TranslationUnavailableReason.ServiceMissing)
            .pageTranslationIssue(ENGINE_NAME) shouldBe
            MangaPageTranslationIssue.Unavailable(TranslationUnavailableReason.ServiceMissing)
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val JAPANESE = LanguageTag.require("ja")
        val KOREAN = LanguageTag.require("ko")
        const val ENGINE_NAME = "LibreTranslate Server"
        val ENGINE = TranslationEngineId("android-system")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = TranslationProviderId("android"),
            providerName = "Android",
            engineName = "Android System Translation",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        val MODEL = TranslationModelDescriptor(TranslationModelId("ja"), JAPANESE, "Japanese")
    }
}
