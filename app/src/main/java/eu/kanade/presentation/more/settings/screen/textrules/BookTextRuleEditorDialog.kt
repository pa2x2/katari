package eu.kanade.presentation.more.settings.screen.textrules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.entry.interactions.reader.settings.BookTextRule
import mihon.entry.interactions.reader.settings.BookTextRuleScope
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.SelectRow
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.ToggleRow
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.util.UUID

/**
 * Adds a rule for one of the [sources], or edits [editing], whose scope stays as it is. [onDelete] is set when
 * editing.
 */
@Composable
internal fun BookTextRuleEditorDialog(
    editing: BookTextRule?,
    sources: List<BookTextRuleScope.Source>,
    onSave: (BookTextRule) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var find by remember { mutableStateOf(editing?.find.orEmpty()) }
    var regex by remember { mutableStateOf(editing?.regex ?: false) }
    var replacing by remember { mutableStateOf(editing?.replaceWith != null) }
    var replacement by remember { mutableStateOf(editing?.replaceWith.orEmpty()) }
    var sourceIndex by remember { mutableIntStateOf(0) }
    val scope = editing?.scope ?: sources.getOrNull(sourceIndex)
    val invalidRegex = regex && find.isNotEmpty() && runCatching { Regex(find) }.isFailure
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(if (editing == null) MR.strings.book_text_rule_add else MR.strings.book_text_rule_edit),
        confirm = DialogAction(
            stringResource(MR.strings.action_save),
            {
                onSave(
                    BookTextRule(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        find = find,
                        scope = requireNotNull(scope),
                        regex = regex,
                        replaceWith = replacement.takeIf { replacing },
                        enabled = editing?.enabled ?: true,
                    ),
                )
            },
            enabled = find.isNotBlank() && !invalidRegex && scope != null,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismiss),
        neutral = onDelete?.let { DialogAction(stringResource(MR.strings.action_delete), it, destructive = true) },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            TextField(
                value = find,
                onValueChange = { find = it },
                label = stringResource(MR.strings.book_text_rule_find),
                isError = invalidRegex,
                supportingText = if (invalidRegex) stringResource(MR.strings.book_text_rule_invalid_regex) else null,
                singleLine = false,
            )
            ToggleRow(
                title = stringResource(MR.strings.book_text_rule_regex),
                checked = regex,
                onCheckedChange = { regex = it },
            )
            Column {
                ChoiceRow(
                    title = stringResource(MR.strings.book_reader_text_rule_hide),
                    selected = !replacing,
                    onClick = { replacing = false },
                )
                ChoiceRow(
                    title = stringResource(MR.strings.book_reader_text_rule_replace_with),
                    selected = replacing,
                    onClick = { replacing = true },
                    below = if (replacing) {
                        {
                            TextField(
                                value = replacement,
                                onValueChange = { replacement = it },
                                label = stringResource(MR.strings.book_reader_text_rule_replacement),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            when {
                editing != null -> SettingsRow(
                    title = stringResource(MR.strings.book_reader_text_rule_applies_to),
                    subtitle = when (val current = editing.scope) {
                        is BookTextRuleScope.Series -> current.title
                        is BookTextRuleScope.Source -> current.name
                    },
                )
                sources.isEmpty() -> SettingsRow(
                    title = stringResource(MR.strings.book_text_rule_no_book_sources),
                )
                else -> SelectRow(
                    label = stringResource(MR.strings.book_text_rule_source),
                    options = sources.map(BookTextRuleScope.Source::name),
                    selectedIndex = sourceIndex,
                    onSelect = { sourceIndex = it },
                )
            }
        }
    }
}
