package eu.kanade.presentation.entry.translation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mihon.entry.interactions.translate.EntryTranslatePlan
import mihon.entry.interactions.translate.EntryTranslatePreparation
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.ui.picker.language.displayName
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
    Sheet(onDismissRequest = actions::dismissSetup) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.chapter_translation_setup_title))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            ) {
                val showTitles = state.preparations.size > 1
                state.preparations.forEach { (entry, preparation) ->
                    if (showTitles) Text(entry.title, role = TextRole.SectionTitle)
                    PlanText(preparation.plan)
                    when (preparation) {
                        is EntryTranslatePreparation.Ready -> Text(
                            text = stringResource(MR.strings.chapter_translation_setup_resolved),
                            role = TextRole.Body,
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
            if (state.working) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KatariTheme.spacing.rowInset),
                )
            }
            SheetActions(
                confirm = DialogAction(
                    label = stringResource(MR.strings.action_translate),
                    onClick = actions::confirmSetup,
                    enabled = state.ready != null && !state.working,
                ),
                dismiss = DialogAction(stringResource(MR.strings.action_cancel), actions::dismissSetup),
            )
        }
    }
    pending?.let { fix -> FixDialog(fix, actions, onDone = { pending = null }) }
}

@Composable
private fun PlanText(plan: EntryTranslatePlan) {
    val source = plan.contentLanguage ?: return
    val target = plan.targetLanguage ?: return
    val languages = stringResource(MR.strings.translation_language_pair, source.displayName(), target.displayName())
    Text(
        text = plan.engine?.let { stringResource(MR.strings.chapter_translation_setup_uses, languages, it.engineName) }
            ?: languages,
        role = TextRole.Body,
        tone = ContentTone.Secondary,
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
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        Text(
            text = row.text,
            role = TextRole.Body,
            modifier = Modifier.weight(1f),
        )
        val fix = row.fix
        if (fix != null && row.fixLabel != null) {
            Button(label = row.fixLabel, onClick = { onFix(fix) }, style = ButtonStyle.Outlined, enabled = enabled)
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
