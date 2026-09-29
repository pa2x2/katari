package mihon.entry.interactions.manga.reader.text.translation

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationProviderDisclosure

/** Why translations cannot be drawn on pages, each with the fix the reader offers for it. */
internal sealed interface MangaPageTranslationIssue {
    /** The page is already in [language], the target; another target is chosen. */
    data class SameLanguage(val language: LanguageTag) : MangaPageTranslationIssue

    /** There is no target to translate into; one is chosen. */
    data object TargetRequired : MangaPageTranslationIssue

    /** The engine, named [engineName] when known, has no [source] → [target] pair; another target is chosen. */
    data class UnsupportedPair(
        val engineName: String?,
        val source: LanguageTag,
        val target: LanguageTag,
    ) : MangaPageTranslationIssue

    /** The engine needs [models] downloaded. */
    data class LanguageDataRequired(
        val engine: TranslationEngineId,
        val engineName: String,
        val models: List<TranslationModelDescriptor>,
    ) : MangaPageTranslationIssue

    /** The engine needs setup it performs itself, such as Android's own language-data prompt. */
    data class SetupRequired(
        val engine: TranslationEngineId,
        val engineName: String,
        val reason: TranslationSystemSetupReason,
    ) : MangaPageTranslationIssue

    /** The engine needs the user to accept [disclosure] before it is used. */
    data class DisclosureRequired(
        val engine: TranslationEngineId,
        val engineName: String,
        val disclosure: TranslationProviderDisclosure,
    ) : MangaPageTranslationIssue

    /** The engine is setting itself up; translations resume once it is done. */
    data object SetupInProgress : MangaPageTranslationIssue

    /** No engine is configured or the chosen one is unavailable; an engine is chosen. */
    data object EngineChoiceRequired : MangaPageTranslationIssue

    /** The engine opens its own surface or needs an action per text, so nothing can be drawn; an engine is chosen. */
    data object EngineUnsupported : MangaPageTranslationIssue

    /** Something Android or the engine controls, fixed from settings. */
    data class Unavailable(val reason: TranslationUnavailableReason) : MangaPageTranslationIssue
}

/**
 * The issue that keeps [preparation] from translating page text, or null when page text can still be translated or
 * skipped. The overlay engine is named [engineName] when known.
 */
internal fun TranslationPreparation.pageTranslationIssue(engineName: String?): MangaPageTranslationIssue? =
    when (this) {
        is TranslationPreparation.Ready,
        is TranslationPreparation.Rejected,
        -> null
        // Page text is always translated from the language pages were read in.
        is TranslationPreparation.SourceUndetermined -> MangaPageTranslationIssue.TargetRequired
        is TranslationPreparation.TargetLanguageRequired -> when (reason) {
            TranslationTargetChoiceReason.NoDefaultTarget -> MangaPageTranslationIssue.TargetRequired
            TranslationTargetChoiceReason.SourceEqualsTarget ->
                sourceLanguage
                    ?.let(MangaPageTranslationIssue::SameLanguage)
                    ?: MangaPageTranslationIssue.TargetRequired
        }
        is TranslationPreparation.ModelDownloadRequired ->
            MangaPageTranslationIssue.LanguageDataRequired(engine, presentation.engineName, models)
        is TranslationPreparation.SystemSetupRequired ->
            MangaPageTranslationIssue.SetupRequired(engine, presentation.engineName, reason)
        is TranslationPreparation.ProviderDisclosureRequired ->
            MangaPageTranslationIssue.DisclosureRequired(engine, presentation.engineName, disclosure)
        is TranslationPreparation.SetupInProgress -> MangaPageTranslationIssue.SetupInProgress
        is TranslationPreparation.EngineChoiceRequired -> MangaPageTranslationIssue.EngineChoiceRequired
        is TranslationPreparation.Unavailable -> when (val reason = reason) {
            is TranslationUnavailableReason.UnsupportedLanguagePair ->
                MangaPageTranslationIssue.UnsupportedPair(engineName, reason.source, reason.target)
            else -> MangaPageTranslationIssue.Unavailable(reason)
        }
    }
