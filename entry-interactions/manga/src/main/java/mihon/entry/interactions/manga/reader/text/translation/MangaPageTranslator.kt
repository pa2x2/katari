package mihon.entry.interactions.manga.reader.text.translation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderOutputMode
import mihon.translation.api.result.TranslationExecution
import mihon.translation.ui.session.language.TranslationLanguageContext

/**
 * Translates recognized page text without user interaction, for drawing translations over the page.
 *
 * Requests use the series' [languages], so drawn translations match the popup. Only engines that answer inline and
 * may run without an explicit action per text qualify; anything that needs the user first is reported instead of
 * attempted. The overlay engine is named by [engineName] in reported issues.
 */
internal class MangaPageTranslator(
    private val feature: TranslationFeature,
    private val languages: TranslationLanguageContext,
    private val engineName: () -> String?,
) {
    /** Emits when the target or engine changes, so translations drawn with the previous ones are out of date. */
    val choicesChanged: Flow<Unit> = languages.choices
        .map { it.target to it.engine }
        .distinctUntilChanged()
        .drop(1)
        .map { }

    suspend fun translate(text: String, language: LanguageTag, pageText: String): MangaPageTranslation {
        val request = languages.request(
            text = text,
            languageContext = TextLanguageResolutionContext(
                surroundingText = pageText.takeIf(String::isNotBlank),
                declaredLanguages = listOf(language),
            ),
            knownSource = language,
        )
        val ready = when (val preparation = feature.prepare(request)) {
            is TranslationPreparation.Ready -> preparation
            else -> return preparation.blocked()
        }
        val presentation = ready.presentation
        if (
            presentation.outputMode != TranslationProviderOutputMode.InlineResult ||
            presentation.invocationPolicy != TranslationInvocationPolicy.Immediate
        ) {
            return MangaPageTranslation.Blocked(MangaPageTranslationIssue.EngineUnsupported)
        }
        return when (val execution = feature.translate(ready.translation)) {
            is TranslationExecution.Success -> MangaPageTranslation.Translated(execution.result.translatedText)
            is TranslationExecution.PreparationChanged -> execution.preparation.blocked()
            is TranslationExecution.ProviderSurfaceOpened ->
                MangaPageTranslation.Blocked(MangaPageTranslationIssue.EngineUnsupported)
            is TranslationExecution.Failed -> MangaPageTranslation.Skipped
        }
    }

    private fun TranslationPreparation.blocked(): MangaPageTranslation =
        pageTranslationIssue(engineName())?.let(MangaPageTranslation::Blocked) ?: MangaPageTranslation.Skipped
}

internal sealed interface MangaPageTranslation {
    data class Translated(val text: String) : MangaPageTranslation

    /** This text could not be translated; the rest of the page can still be. */
    data object Skipped : MangaPageTranslation

    /** Nothing can be translated until [issue] is resolved. */
    data class Blocked(val issue: MangaPageTranslationIssue) : MangaPageTranslation
}
