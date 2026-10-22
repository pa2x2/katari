package eu.kanade.presentation.entry.translation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mihon.entry.interactions.translate.EntryTranslatePlan
import mihon.entry.interactions.translate.EntryTranslatePreparation
import mihon.entry.interactions.translate.EntryTranslateRequirement
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.SettingsRow
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
                    .padding(vertical = KatariTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            ) {
                val showTitles = state.preparations.size > 1
                state.preparations.forEach { (entry, preparation) ->
                    if (showTitles) RowGroupHeader(entry.title)
                    PlanText(preparation.plan)
                    when (preparation) {
                        is EntryTranslatePreparation.Ready -> Text(
                            text = stringResource(MR.strings.chapter_translation_setup_resolved),
                            modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset),
                            role = TextRole.Body,
                        )
                        is EntryTranslatePreparation.Blocked -> RequirementRows(
                            requirements = preparation.requirements,
                            enabled = !state.working,
                            onFix = { requirement, fix ->
                                when (fix) {
                                    is EntryTranslateFix.Direct -> runFix(context, actions, requirement, fix)
                                    is EntryTranslateFix.InDialog -> pending = PendingFix(entry, requirement, fix)
                                }
                            },
                        )
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
        modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset),
        role = TextRole.Body,
        tone = ContentTone.Secondary,
    )
}

/** One entry's blockers as rows in a card. */
@Composable
private fun RequirementRows(
    requirements: List<EntryTranslateRequirement>,
    enabled: Boolean,
    onFix: (EntryTranslateRequirement, EntryTranslateFix) -> Unit,
) {
    RowGroup(
        rows = requirements.map<EntryTranslateRequirement, @Composable () -> Unit> { requirement ->
            {
                RequirementRow(
                    row = entryTranslateRequirementRow(requirement),
                    enabled = enabled,
                    onFix = { fix -> onFix(requirement, fix) },
                )
            }
        },
    )
}

/** A blocker as a row whose action is its fix, with the fix named at its end. */
@Composable
private fun RequirementRow(
    row: EntryTranslateRequirementRow,
    enabled: Boolean,
    onFix: (EntryTranslateFix) -> Unit,
) {
    val fix = row.fix
    val fixLabel = row.fixLabel
    SettingsRow(
        title = row.text,
        trailing = if (fix != null && fixLabel != null) {
            { Button(label = fixLabel, onClick = { onFix(fix) }, style = ButtonStyle.Tonal, enabled = enabled) }
        } else {
            null
        },
        enabled = enabled,
        onClick = fix?.let { { onFix(it) } },
    )
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
