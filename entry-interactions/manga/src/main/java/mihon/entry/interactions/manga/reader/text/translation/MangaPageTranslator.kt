package mihon.entry.interactions.manga.reader.text.translation

import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderOutputMode
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.result.TranslationExecution

/**
 * Translates recognized page text without user interaction, for drawing translations over the page.
 *
 * Only engines that answer inline and may run without an explicit action per text qualify; anything that needs the
 * user first is reported instead of attempted.
 */
internal class MangaPageTranslator(
    private val feature: TranslationFeature,
) {
    suspend fun translate(text: String, language: LanguageTag, pageText: String): MangaPageTranslation {
        val request = TranslationRequest(
            text = text,
            sourceLanguage = TranslationSourceLanguageSelection.Explicit(language),
            languageContext = TextLanguageResolutionContext(
                surroundingText = pageText.takeIf(String::isNotBlank),
                declaredLanguages = listOf(language),
            ),
        )
        val ready = when (val preparation = feature.prepare(request)) {
            is TranslationPreparation.Ready -> preparation
            is TranslationPreparation.Rejected -> return MangaPageTranslation.Skipped
            else -> return MangaPageTranslation.SetupRequired
        }
        val presentation = ready.presentation
        if (
            presentation.outputMode != TranslationProviderOutputMode.InlineResult ||
            presentation.invocationPolicy != TranslationInvocationPolicy.Immediate
        ) {
            return MangaPageTranslation.EngineUnsupported
        }
        return when (val execution = feature.translate(ready.translation)) {
            is TranslationExecution.Success -> MangaPageTranslation.Translated(execution.result.translatedText)
            is TranslationExecution.PreparationChanged -> MangaPageTranslation.SetupRequired
            is TranslationExecution.ProviderSurfaceOpened -> MangaPageTranslation.EngineUnsupported
            is TranslationExecution.Failed -> MangaPageTranslation.Skipped
        }
    }
}

internal sealed interface MangaPageTranslation {
    data class Translated(val text: String) : MangaPageTranslation

    /** This text could not be translated; the rest of the page can still be. */
    data object Skipped : MangaPageTranslation

    /** The translation engine needs the user before it can translate anything. */
    data object SetupRequired : MangaPageTranslation

    /** The chosen engine cannot produce text to draw on the page. */
    data object EngineUnsupported : MangaPageTranslation
}
