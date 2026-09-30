package eu.kanade.presentation.entry.translation

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.presentation.components.AdaptiveSheet
import mihon.entry.interactions.translate.EntryTranslatePlan
import mihon.entry.interactions.translate.EntryTranslatePreparation
import mihon.entry.interactions.translate.EntryTranslateRequirement
import mihon.model.artifacts.ui.approval.ModelArtifactDownloadApprovalDialog
import mihon.text.recognition.api.host.openTextRecognitionPipelineChoice
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.ui.approval.TextRecognitionPlatformModelsDialog
import mihon.translation.api.host.openTranslationSettings
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.ui.picker.language.TranslationLanguagePickerList
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.picker.language.translationLanguageOptions
import mihon.translation.ui.picker.language.translationLanguageOptionsOf
import mihon.translation.ui.presentation.TranslationPickerSheet
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Lists everything that blocks translating the chosen chapters, each with its fix, and what translation will use. The
 * chapters queue when the user confirms once nothing is left.
 */
@Composable
fun EntryTranslateSetupSheet(
    state: EntryTranslateSetupState,
    actions: EntryTranslateSetupActions,
) {
    RefreshOnResume(actions::refreshSetup)
    val context = LocalContext.current
    var pending by remember { mutableStateOf<PendingFix?>(null) }
    AdaptiveSheet(onDismissRequest = actions::dismissSetup) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(MR.strings.chapter_translation_setup_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val showTitles = state.preparations.size > 1
                state.preparations.forEach { (entry, preparation) ->
                    if (showTitles) Text(entry.title, style = MaterialTheme.typography.titleSmall)
                    PlanText(preparation.plan)
                    when (preparation) {
                        is EntryTranslatePreparation.Ready -> Text(
                            text = stringResource(MR.strings.chapter_translation_setup_resolved),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        is EntryTranslatePreparation.Blocked -> preparation.requirements.forEach { requirement ->
                            RequirementRow(
                                row = entryTranslateRequirementRow(requirement),
                                enabled = !state.working,
                                onFix = { fix ->
                                    when (fix) {
                                        is EntryTranslateFix.Direct -> runFix(context, actions, requirement, fix)
                                        is EntryTranslateFix.InDialog -> pending = PendingFix(entry, requirement, fix)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            if (state.working) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = actions::dismissSetup) {
                    Text(stringResource(MR.strings.action_cancel))
                }
                Button(
                    onClick = actions::confirmSetup,
                    enabled = state.ready != null && !state.working,
                ) {
                    Text(stringResource(MR.strings.action_translate))
                }
            }
        }
    }
    pending?.let { fix -> FixDialog(fix, actions, onDone = { pending = null }) }
}

private class PendingFix(
    val entry: Entry,
    val requirement: EntryTranslateRequirement,
    val fix: EntryTranslateFix.InDialog,
)

private fun runFix(
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

@Composable
private fun PlanText(plan: EntryTranslatePlan) {
    val source = plan.contentLanguage ?: return
    val target = plan.targetLanguage ?: return
    val languages = stringResource(MR.strings.translation_language_pair, source.displayName(), target.displayName())
    Text(
        text = plan.engine?.let { stringResource(MR.strings.chapter_translation_setup_uses, languages, it.engineName) }
            ?: languages,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RequirementRow(
    row: EntryTranslateRequirementRow,
    enabled: Boolean,
    onFix: (EntryTranslateFix) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = row.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        val fix = row.fix
        if (fix != null && row.fixLabel != null) {
            OutlinedButton(onClick = { onFix(fix) }, enabled = enabled) {
                Text(row.fixLabel)
            }
        }
    }
}

/** Shows the dialog or picker [fix] needs, then calls [onDone]. */
@Composable
private fun FixDialog(fix: PendingFix, actions: EntryTranslateSetupActions, onDone: () -> Unit) {
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
            AlertDialog(
                onDismissRequest = onDone,
                title = { Text(required.disclosure.title) },
                text = { Text(required.disclosure.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            actions.acknowledgeDisclosure(required.engine, required.disclosure)
                            onDone()
                        },
                    ) {
                        Text(required.disclosure.confirmationLabel)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDone) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
    }
}

@Composable
private fun RefreshOnResume(refresh: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
