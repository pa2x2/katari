package eu.kanade.presentation.entry.translation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import mihon.entry.interactions.translate.EntryTranslateRequirement
import mihon.model.artifacts.ui.approval.ModelArtifactDownloadApprovalDialog
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.text.recognition.api.host.openTextRecognitionPipelineChoice
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.ui.approval.TextRecognitionPlatformModelsDialog
import mihon.translation.api.host.openTranslationSettings
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.ui.picker.language.TranslationLanguagePickerList
import mihon.translation.ui.picker.language.translationLanguageOptions
import mihon.translation.ui.picker.language.translationLanguageOptionsOf
import mihon.translation.ui.presentation.TranslationPickerSheet
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

// How each fix offered by the translation setup sheet is carried out: directly, or through its own dialog or picker.

internal class PendingFix(
    val entry: Entry,
    val requirement: EntryTranslateRequirement,
    val fix: EntryTranslateFix.InDialog,
)

internal fun runFix(
    context: Context,
    actions: EntryTranslateSetupActions,
    requirement: EntryTranslateRequirement,
    fix: EntryTranslateFix.Direct,
) {
    val recognition = (requirement as? EntryTranslateRequirement.Recognition)?.requirement
    val translation = (requirement as? EntryTranslateRequirement.Translation)?.requirement
    when (fix) {
        EntryTranslateFix.ChoosePipeline -> context.openTextRecognitionPipelineChoice(
            (recognition as TextRecognitionPreparation.PipelineChoiceRequired).language,
        )
        EntryTranslateFix.DownloadTranslationModels -> {
            val required = translation as TranslationPreparation.ModelDownloadRequired
            actions.downloadTranslationModels(required.engine, required.models)
        }
        EntryTranslateFix.OpenTranslationSetup -> actions.openTranslationSetup(
            (translation as TranslationPreparation.SystemSetupRequired).engine,
        )
        EntryTranslateFix.OpenTranslationSettings -> context.openTranslationSettings()
    }
}

/** Shows the dialog or picker [fix] needs, then calls [onDone]. */
@Composable
internal fun FixDialog(fix: PendingFix, actions: EntryTranslateSetupActions, onDone: () -> Unit) {
    val recognition = (fix.requirement as? EntryTranslateRequirement.Recognition)?.requirement
    val translation = (fix.requirement as? EntryTranslateRequirement.Translation)?.requirement
    when (fix.fix) {
        EntryTranslateFix.ChoosePageLanguage -> {
            val languages = (recognition as TextRecognitionPreparation.LanguageRequired).supportedLanguages
            TranslationPickerSheet(
                title = stringResource(MR.strings.chapter_translation_page_language),
                onDismiss = onDone,
            ) {
                TranslationLanguagePickerList(
                    options = remember(languages) { translationLanguageOptionsOf(languages) },
                    selected = null,
                    onSelect = {
                        actions.choosePageLanguage(fix.entry, it)
                        onDone()
                    },
                )
            }
        }
        EntryTranslateFix.ChooseTargetLanguage -> TranslationPickerSheet(
            title = stringResource(MR.strings.chapter_translation_target_required),
            onDismiss = onDone,
        ) {
            TranslationLanguagePickerList(
                options = remember { translationLanguageOptions() },
                selected = null,
                onSelect = {
                    actions.chooseTargetLanguage(fix.entry, it)
                    onDone()
                },
            )
        }
        EntryTranslateFix.ApproveRecognitionModels -> ModelArtifactDownloadApprovalDialog(
            artifacts = (recognition as TextRecognitionPreparation.ModelsRequired).models,
            onApprove = {
                actions.approveRecognitionModels(it)
                onDone()
            },
            onDismiss = onDone,
        )
        EntryTranslateFix.ApprovePlatformModels -> {
            val required = recognition as TextRecognitionPreparation.PlatformModelsRequired
            TextRecognitionPlatformModelsDialog(
                description = required.description,
                approximateSizeBytes = required.approximateSizeBytes,
                onApprove = {
                    actions.installPlatformModels(required.component, required.language)
                    onDone()
                },
                onDismiss = onDone,
            )
        }
        EntryTranslateFix.AcknowledgeDisclosure -> {
            val required = translation as TranslationPreparation.ProviderDisclosureRequired
            ConfirmDialog(
                title = required.disclosure.title,
                text = required.disclosure.message,
                confirmLabel = required.disclosure.confirmationLabel,
                onConfirm = {
                    actions.acknowledgeDisclosure(required.engine, required.disclosure)
                    onDone()
                },
                onDismissRequest = onDone,
            )
        }
    }
}
