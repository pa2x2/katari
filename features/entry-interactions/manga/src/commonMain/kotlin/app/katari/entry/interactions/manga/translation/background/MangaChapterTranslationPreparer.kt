package app.katari.entry.interactions.manga.translation.background

import app.katari.entry.interactions.translate.EntryTranslatePlan
import app.katari.entry.interactions.translate.EntryTranslatePreparation
import app.katari.entry.interactions.translate.EntryTranslateRequirement
import app.katari.entry.interactions.translate.EntryTranslateSetup
import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.TextRecognitionFeature
import app.katari.text.recognition.api.preparation.TextRecognitionRequirement
import app.katari.text.recognition.api.preparation.TextRecognitionSetupPreparation
import app.katari.text.recognition.api.request.TextRecognitionSetupRequest
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.engine.TranslationEngineSelection
import app.katari.translation.api.preparation.TranslationPreparation
import app.katari.translation.api.preparation.TranslationRequirement
import app.katari.translation.api.preparation.TranslationRoutePreparation
import app.katari.translation.api.provider.TranslationInvocationPolicy
import app.katari.translation.api.provider.TranslationProviderOutputMode
import app.katari.translation.api.provider.TranslationProviderPresentation
import app.katari.translation.api.request.TranslationRouteRequest
import app.katari.translation.api.request.TranslationTargetLanguageSelection

/**
 * Settles what a series' chapters are translated with before they are queued: the page language and the profile's
 * recognition pipeline for it, and the translation route to the series' or profile's target with the profile's engine.
 * These are the choices the reader makes, so a chapter translated ahead reads like one translated while reading.
 */
internal class MangaChapterTranslationPreparer(
    private val recognition: TextRecognitionFeature,
    private val translation: TranslationFeature,
    private val defaultTarget: () -> LanguageTag?,
) {
    /**
     * @param pageLanguage the series' chosen page language, else its source's; `null` when neither is known.
     * @param targetLanguage the series' chosen target; `null` follows the profile.
     */
    suspend fun prepare(pageLanguage: LanguageTag?, targetLanguage: LanguageTag?): EntryTranslatePreparation {
        val requirements = mutableListOf<EntryTranslateRequirement>()
        val pipeline = when (val setup = recognition.prepare(TextRecognitionSetupRequest(pageLanguage))) {
            is TextRecognitionSetupPreparation.Ready -> setup.pipeline
            is TextRecognitionRequirement -> {
                requirements += EntryTranslateRequirement.Recognition(setup)
                null
            }
        }
        // Without the page language there is no route to check yet; it is asked for first.
        pageLanguage ?: return EntryTranslatePreparation.Blocked(
            requirements,
            EntryTranslatePlan(
                contentLanguage = null,
                targetLanguage = targetLanguage ?: defaultTarget(),
                engine = null,
            ),
        )
        val route = translation.prepareRoute(
            TranslationRouteRequest(
                sourceLanguage = pageLanguage,
                targetLanguage = targetLanguage
                    ?.let(TranslationTargetLanguageSelection::Explicit)
                    ?: TranslationTargetLanguageSelection.Default,
                engine = TranslationEngineSelection.ProfileDefault,
            ),
        )
        val ready = when (route) {
            is TranslationRoutePreparation.Ready -> route.takeIf { it.presentation.answersInline() }
                ?: null.also { requirements += EntryTranslateRequirement.EngineNeedsUser(route.presentation) }
            is TranslationRequirement -> null.also { requirements += EntryTranslateRequirement.Translation(route) }
        }
        // A blocked route still names the target and engine it would use, so the user sees what they are setting up.
        val plan = EntryTranslatePlan(
            contentLanguage = pageLanguage,
            targetLanguage = ready?.route?.targetLanguage ?: targetLanguage ?: defaultTarget(),
            engine = when (route) {
                is TranslationRoutePreparation.Ready -> route.presentation
                is TranslationRequirement -> route.engine()
            },
        )
        if (ready == null || pipeline == null) {
            return EntryTranslatePreparation.Blocked(requirements, plan)
        }
        return EntryTranslatePreparation.Ready(
            setup = EntryTranslateSetup(
                contentLanguage = pageLanguage,
                targetLanguage = ready.route.targetLanguage,
                engine = ready.route.engine,
                recognition = pipeline,
            ),
            plan = plan,
        )
    }
}

private fun TranslationRequirement.engine(): TranslationProviderPresentation? = when (this) {
    is TranslationPreparation.ProviderDisclosureRequired -> presentation
    is TranslationPreparation.ModelDownloadRequired -> presentation
    is TranslationPreparation.SystemSetupRequired -> presentation
    is TranslationPreparation.SetupInProgress -> presentation
    is TranslationPreparation.TargetLanguageRequired,
    is TranslationPreparation.EngineChoiceRequired,
    is TranslationPreparation.Unavailable,
    -> null
}

/** Whether the engine returns translations to the app without the user acting on each text, as background work needs. */
internal fun TranslationProviderPresentation.answersInline(): Boolean =
    outputMode == TranslationProviderOutputMode.InlineResult &&
        invocationPolicy == TranslationInvocationPolicy.Immediate
