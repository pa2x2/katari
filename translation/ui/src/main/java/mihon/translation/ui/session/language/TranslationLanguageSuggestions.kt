package mihon.translation.ui.session.language

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.ui.picker.language.TranslationLanguageRole
import mihon.translation.ui.picker.language.supportsPair
import mihon.translation.ui.picker.language.supportsSelection

/**
 * Languages offered as one-tap answers to [preparation], a translation that waits for a language choice, checked
 * against what [engineName] can translate.
 */
data class TranslationLanguageSuggestions(
    val preparation: TranslationPreparation,
    val languages: List<LanguageTag>,
    val engineName: String?,
)

/** Whether the translation can continue once the reader chooses a source or target language. */
fun TranslationPreparation.awaitsLanguageChoice(): Boolean = when (this) {
    is TranslationPreparation.SourceUndetermined,
    is TranslationPreparation.TargetLanguageRequired,
    -> true
    is TranslationPreparation.Unavailable -> reason is TranslationUnavailableReason.UnsupportedLanguagePair
    is TranslationPreparation.Ready,
    is TranslationPreparation.ProviderDisclosureRequired,
    is TranslationPreparation.ModelDownloadRequired,
    is TranslationPreparation.SystemSetupRequired,
    is TranslationPreparation.SetupInProgress,
    is TranslationPreparation.EngineChoiceRequired,
    is TranslationPreparation.Rejected,
    -> false
}

/**
 * The languages to offer for [preparation], or null when it does not [await a language choice][awaitsLanguageChoice].
 *
 * Sources come from the languages the text was suggested to be in, then [recentLanguages]. Targets come from the
 * [defaultTarget], then [recentLanguages], and never repeat the text's language or a target that just failed. Only
 * languages [support] can translate are offered; nothing is offered while [support] is still unknown.
 */
internal fun suggestedLanguages(
    preparation: TranslationPreparation,
    recentLanguages: List<LanguageTag>,
    defaultTarget: LanguageTag?,
    support: TranslationLanguageSupport?,
    limit: Int = SUGGESTED_LANGUAGES_LIMIT,
): List<LanguageTag>? {
    if (!preparation.awaitsLanguageChoice()) return null
    if (support == null) return emptyList()
    val targets = listOfNotNull(defaultTarget) + recentLanguages
    val candidates = when (preparation) {
        is TranslationPreparation.SourceUndetermined ->
            (preparation.suggestedLanguages + recentLanguages).filter {
                support.supportsSelection(TranslationLanguageRole.Source, it, counterpart = null)
            }
        is TranslationPreparation.TargetLanguageRequired ->
            targets.filter { support.acceptsTarget(it, preparation.sourceLanguage) }
        is TranslationPreparation.Unavailable -> {
            val pair = preparation.reason as? TranslationUnavailableReason.UnsupportedLanguagePair ?: return null
            targets.filter { it != pair.target && support.acceptsTarget(it, pair.source) }
        }
        else -> return null
    }
    return candidates.distinct().take(limit)
}

private fun TranslationLanguageSupport.acceptsTarget(language: LanguageTag, source: LanguageTag?): Boolean =
    if (source != null) {
        supportsPair(source, language)
    } else {
        supportsSelection(TranslationLanguageRole.Target, language, counterpart = null)
    }

private const val SUGGESTED_LANGUAGES_LIMIT = 3
