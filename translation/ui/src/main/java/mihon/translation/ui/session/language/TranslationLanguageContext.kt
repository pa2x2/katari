package mihon.translation.ui.session.language

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.language.TranslationDefaultTarget
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection

/**
 * The languages and engine every translation uses while one piece of content is read.
 *
 * A choice made in any translation surface applies to every later request. Source and target choices are kept by
 * the [store] for the content when there is one; the engine choice lasts for the session only.
 */
class TranslationLanguageContext(
    private val defaultTarget: () -> TranslationDefaultTarget?,
    private val store: TranslationLanguageStore?,
    private val scope: CoroutineScope,
) {
    private val mutableChoices = MutableStateFlow(TranslationLanguageChoices())
    val choices: StateFlow<TranslationLanguageChoices> = mutableChoices.asStateFlow()

    /** Whether source and target choices are kept for the content, rather than lasting for the session. */
    val keepsChoices: Boolean = store != null

    init {
        store?.let { store ->
            scope.launch {
                store.languages.collect { stored ->
                    mutableChoices.update { it.copy(source = stored.source, target = stored.target) }
                }
            }
        }
    }

    /**
     * Builds the request for [text] from the current choices. [knownSource] is the language the host already knows
     * the text is in, such as the language pages were recognized in; it takes precedence over the source choice.
     */
    fun request(
        text: String,
        languageContext: TextLanguageResolutionContext,
        knownSource: LanguageTag? = null,
    ): TranslationRequest {
        val current = mutableChoices.value
        return TranslationRequest(
            text = text,
            sourceLanguage = sourceSelection(knownSource ?: current.source),
            targetLanguage = targetSelection(current.target),
            engine = current.engine
                ?.let(TranslationEngineSelection::Explicit)
                ?: TranslationEngineSelection.ProfileDefault,
            languageContext = languageContext,
        )
    }

    /** Pins the source, or returns to detection when [language] is null. */
    fun selectSource(language: LanguageTag?): TranslationSourceLanguageSelection {
        mutableChoices.update { it.copy(source = language) }
        store?.let { store -> scope.launch { store.setSourceLanguage(language) } }
        return sourceSelection(language)
    }

    /**
     * Pins the target, or follows the profile's target again when [language] is null. Choosing the language the
     * profile's target already resolves to follows the profile, so the content does not keep a redundant choice.
     */
    fun selectTarget(language: LanguageTag?): TranslationTargetLanguageSelection {
        val kept = language?.takeUnless { it == defaultTarget()?.language }
        mutableChoices.update { it.copy(target = kept) }
        store?.let { store -> scope.launch { store.setTargetLanguage(kept) } }
        return targetSelection(kept)
    }

    /** The target requests use with [choices]: the kept one, else the default; null when there is neither. */
    fun effectiveTarget(choices: TranslationLanguageChoices = this.choices.value): TranslationEffectiveTarget? =
        choices.target?.let(TranslationEffectiveTarget::Kept)
            ?: defaultTarget()?.let(TranslationEffectiveTarget::Default)

    /** Uses [engine] for the rest of the session, or the profile's engine when it is null. */
    fun selectEngine(engine: TranslationEngineId?) {
        mutableChoices.update { it.copy(engine = engine) }
    }

    private fun sourceSelection(language: LanguageTag?): TranslationSourceLanguageSelection =
        language?.let(TranslationSourceLanguageSelection::Explicit) ?: TranslationSourceLanguageSelection.Automatic

    private fun targetSelection(language: LanguageTag?): TranslationTargetLanguageSelection =
        language?.let(TranslationTargetLanguageSelection::Explicit) ?: TranslationTargetLanguageSelection.Default
}

/** Current choices; a null value follows its default. */
data class TranslationLanguageChoices(
    val source: LanguageTag? = null,
    val target: LanguageTag? = null,
    val engine: TranslationEngineId? = null,
)

/** The target language requests use, and whether it was kept for the content or follows the default. */
sealed interface TranslationEffectiveTarget {
    val language: LanguageTag

    data class Kept(override val language: LanguageTag) : TranslationEffectiveTarget

    data class Default(val target: TranslationDefaultTarget) : TranslationEffectiveTarget {
        override val language: LanguageTag get() = target.language
    }
}
