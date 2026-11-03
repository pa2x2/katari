package eu.kanade.presentation.entry.translation

import androidx.compose.runtime.Composable
import mihon.entry.interactions.translate.EntryTranslateRequirement
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.preparation.TextRecognitionUnavailableReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How the prerequisites sheet resolves one requirement. */
sealed interface EntryTranslateFix {
    /** Fixes the user completes in a dialog or picker over the sheet. */
    sealed interface InDialog : EntryTranslateFix

    /** Fixes that act at once, some by leaving for settings; the sheet prepares again when the user returns. */
    sealed interface Direct : EntryTranslateFix

    data object ChoosePageLanguage : InDialog

    data object ChooseTargetLanguage : InDialog

    data object ApproveRecognitionModels : InDialog

    data object ApprovePlatformModels : InDialog

    data object AcknowledgeDisclosure : InDialog

    data object ChoosePipeline : Direct

    data object DownloadTranslationModels : Direct

    data object OpenTranslationSetup : Direct

    data object OpenTranslationSettings : Direct
}

/** What the sheet says about a requirement, and the fix it offers with its button label, if any. */
class EntryTranslateRequirementRow(
    val text: String,
    val fix: EntryTranslateFix?,
    val fixLabel: String?,
)

@Composable
fun entryTranslateRequirementRow(requirement: EntryTranslateRequirement): EntryTranslateRequirementRow {
    val choose = stringResource(MR.strings.action_choose)
    val download = stringResource(MR.strings.action_download)
    val settings = stringResource(MR.strings.label_settings)
    return when (requirement) {
        is EntryTranslateRequirement.Recognition -> when (val req = requirement.requirement) {
            is TextRecognitionPreparation.LanguageRequired -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_page_language),
                EntryTranslateFix.ChoosePageLanguage,
                choose,
            )
            is TextRecognitionPreparation.PipelineChoiceRequired -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_pipeline_required, req.language.displayName()),
                EntryTranslateFix.ChoosePipeline,
                choose,
            )
            is TextRecognitionPreparation.ModelsRequired -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_recognition_models),
                EntryTranslateFix.ApproveRecognitionModels,
                download,
            )
            is TextRecognitionPreparation.PlatformModelsRequired -> EntryTranslateRequirementRow(
                req.description,
                EntryTranslateFix.ApprovePlatformModels,
                download,
            )
            is TextRecognitionPreparation.Unavailable -> EntryTranslateRequirementRow(
                when (val reason = req.reason) {
                    is TextRecognitionUnavailableReason.UnsupportedLanguage -> stringResource(
                        MR.strings.chapter_translation_unsupported_language,
                        reason.language.displayName(),
                    )
                    is TextRecognitionUnavailableReason.ComponentUnavailable -> stringResource(
                        MR.strings.chapter_translation_recognition_unavailable,
                        reason.reason,
                    )
                },
                fix = null,
                fixLabel = null,
            )
        }
        is EntryTranslateRequirement.Translation -> when (val req = requirement.requirement) {
            is TranslationPreparation.ProviderDisclosureRequired -> EntryTranslateRequirementRow(
                req.disclosure.title,
                EntryTranslateFix.AcknowledgeDisclosure,
                req.disclosure.confirmationLabel,
            )
            is TranslationPreparation.ModelDownloadRequired -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_translation_models),
                EntryTranslateFix.DownloadTranslationModels,
                download,
            )
            is TranslationPreparation.SystemSetupRequired -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_system_setup, req.presentation.engineName),
                EntryTranslateFix.OpenTranslationSetup,
                stringResource(MR.strings.action_set_up),
            )
            is TranslationPreparation.SetupInProgress -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_setup_in_progress, req.presentation.engineName),
                fix = null,
                fixLabel = null,
            )
            is TranslationPreparation.TargetLanguageRequired -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_target_required),
                EntryTranslateFix.ChooseTargetLanguage,
                choose,
            )
            is TranslationPreparation.EngineChoiceRequired -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_engine_required),
                EntryTranslateFix.OpenTranslationSettings,
                settings,
            )
            is TranslationPreparation.Unavailable -> EntryTranslateRequirementRow(
                stringResource(MR.strings.chapter_translation_unavailable),
                EntryTranslateFix.OpenTranslationSettings,
                settings,
            )
        }
        is EntryTranslateRequirement.EngineNeedsUser -> EntryTranslateRequirementRow(
            stringResource(MR.strings.chapter_translation_engine_needs_user, requirement.engine.engineName),
            EntryTranslateFix.OpenTranslationSettings,
            settings,
        )
    }
}
