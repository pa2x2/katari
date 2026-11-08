package app.katari.entry.interactions.translate

import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.preparation.TextRecognitionRequirement
import app.katari.translation.api.preparation.TranslationRequirement
import app.katari.translation.api.provider.TranslationProviderPresentation

/** Whether a series' chapters can be queued for translation now, and with what. */
sealed interface EntryTranslatePreparation {
    val plan: EntryTranslatePlan

    /** Chapters queued with [setup] can be translated without asking the user anything. */
    data class Ready(
        val setup: EntryTranslateSetup,
        override val plan: EntryTranslatePlan,
    ) : EntryTranslatePreparation

    /** Every [requirements] must be resolved first; preparing again after each fix may reveal more. */
    data class Blocked(
        val requirements: List<EntryTranslateRequirement>,
        override val plan: EntryTranslatePlan,
    ) : EntryTranslatePreparation {
        init {
            require(requirements.isNotEmpty())
        }
    }
}

/** What translation would use, as far as it is known yet. */
data class EntryTranslatePlan(
    val contentLanguage: LanguageTag?,
    val targetLanguage: LanguageTag?,
    val engine: TranslationProviderPresentation?,
)

sealed interface EntryTranslateRequirement {
    data class Recognition(val requirement: TextRecognitionRequirement) : EntryTranslateRequirement

    data class Translation(val requirement: TranslationRequirement) : EntryTranslateRequirement

    /** The engine needs the user for each text or shows results itself, so it cannot translate in the background. */
    data class EngineNeedsUser(val engine: TranslationProviderPresentation) : EntryTranslateRequirement
}
