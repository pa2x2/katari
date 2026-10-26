package mihon.entry.interactions.book.document.reader.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.entry.interactions.reader.settings.BookTextRule
import mihon.entry.interactions.reader.settings.BookTextRuleScope
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.util.UUID

/**
 * Turns text selected in a chapter into rules that hide it, or replace it, from now on in [series] or everything from
 * [source]. A selection across paragraphs becomes one rule per paragraph, since rules work within one; replacing then
 * isn't offered, as it would repeat the replacement in each.
 */
@Composable
internal fun BookDocumentHideTextDialog(
    selectedText: String,
    series: BookTextRuleScope.Series,
    source: BookTextRuleScope.Source,
    onSave: (List<BookTextRule>) -> Unit,
    onDismiss: () -> Unit,
) {
    val paragraphs = remember(selectedText) {
        selectedText.lines().map(String::trim).filter(String::isNotEmpty)
    }
    var replacing by remember { mutableStateOf(false) }
    var replacement by remember { mutableStateOf("") }
    var wholeSource by remember { mutableStateOf(false) }
    val canReplace = paragraphs.size == 1
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(MR.strings.book_reader_text_rule_title),
        confirm = DialogAction(
            stringResource(
                if (replacing) MR.strings.book_reader_text_rule_replace else MR.strings.book_reader_text_rule_hide,
            ),
            {
                val scope = if (wholeSource) source else series
                onSave(
                    paragraphs.map { paragraph ->
                        BookTextRule(
                            id = UUID.randomUUID().toString(),
                            find = paragraph,
                            scope = scope,
                            replaceWith = replacement.takeIf { replacing },
                        )
                    },
                )
            },
            enabled = paragraphs.isNotEmpty(),
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismiss),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(
                selectedText.trim(),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
                maxLines = QUOTE_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Column {
                ChoiceRow(
                    title = stringResource(MR.strings.book_reader_text_rule_hide),
                    selected = !replacing,
                    onClick = { replacing = false },
                )
                ChoiceRow(
                    title = stringResource(MR.strings.book_reader_text_rule_replace_with),
                    subtitle = if (canReplace) null else stringResource(MR.strings.book_reader_text_rule_one_paragraph),
                    selected = replacing,
                    onClick = { replacing = true },
                    enabled = canReplace,
                    below = if (replacing) {
                        {
                            TextField(
                                value = replacement,
                                onValueChange = { replacement = it },
                                label = stringResource(MR.strings.book_reader_text_rule_replacement),
                                singleLine = true,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            RowGroupHeader(stringResource(MR.strings.book_reader_text_rule_applies_to))
            Column {
                ChoiceRow(
                    title = stringResource(MR.strings.book_reader_text_rule_this_series),
                    subtitle = series.title,
                    selected = !wholeSource,
                    onClick = { wholeSource = false },
                )
                ChoiceRow(
                    title = stringResource(MR.strings.book_reader_text_rule_this_source),
                    subtitle = source.name,
                    selected = wholeSource,
                    onClick = { wholeSource = true },
                )
            }
        }
    }
}

private const val QUOTE_LINES = 4
