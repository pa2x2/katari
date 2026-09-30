package mihon.entry.interactions.manga.reader.text.translation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.preparation.TranslationRequirement
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderOutputMode
import mihon.translation.api.request.TranslationBatch
import mihon.translation.api.request.TranslationContext
import mihon.translation.api.result.TranslationBatchUpdate
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

    /**
     * Translates [texts] of a page in [language] together and in their [context], reporting each translation as soon
     * as it is known. A text that could not be translated is not reported; the rest of the page still is, unless the
     * engine failed, which is reported as an issue of its own.
     */
    fun translate(texts: List<String>, language: LanguageTag, context: TranslationContext): Flow<MangaPageTranslation> =
        flow {
            if (texts.isEmpty()) return@flow
            val route = when (val preparation = feature.prepareRoute(languages.routeRequest(language))) {
                is TranslationRoutePreparation.Ready -> preparation
                is TranslationRequirement -> {
                    preparation.blocked()?.let { emit(it) }
                    return@flow
                }
            }
            if (
                route.presentation.outputMode != TranslationProviderOutputMode.InlineResult ||
                route.presentation.invocationPolicy != TranslationInvocationPolicy.Immediate
            ) {
                emit(MangaPageTranslation.Blocked(MangaPageTranslationIssue.EngineUnsupported))
                return@flow
            }
            feature.translateBatch(TranslationBatch(route.route, texts, context)).collect { update ->
                when (update) {
                    is TranslationBatchUpdate.Translated ->
                        emit(MangaPageTranslation.Translated(update.index, update.text))
                    is TranslationBatchUpdate.Failed -> Unit
                    is TranslationBatchUpdate.EngineFailed -> {
                        val issue = MangaPageTranslationIssue.EngineFailed(update.failure.message)
                        emit(MangaPageTranslation.Blocked(issue))
                    }
                    is TranslationBatchUpdate.Blocked -> update.requirement.blocked()?.let { emit(it) }
                }
            }
        }

    private fun TranslationRequirement.blocked(): MangaPageTranslation.Blocked? =
        pageTranslationIssue(engineName())?.let(MangaPageTranslation::Blocked)
}

internal sealed interface MangaPageTranslation {
    /** The text at [index] of the translated texts reads [text]. */
    data class Translated(val index: Int, val text: String) : MangaPageTranslation

    /** Nothing can be translated until [issue] is resolved. */
    data class Blocked(val issue: MangaPageTranslationIssue) : MangaPageTranslation
}
