package eu.kanade.presentation.more.settings.screen.textrules

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import app.katari.entry.interactions.reader.settings.BookTextRule
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.icons.materialsymbols.rounded.Edit
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.control.Switch
import app.katari.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One text rule: what it finds and what it does there, with a switch that turns it off without deleting it. Where it
 * applies is the header of its group.
 */
@Composable
internal fun BookTextRuleItem(
    rule: BookTextRule,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SettingsRow(
        title = if (rule.regex) "/${rule.find}/" else rule.find,
        subtitle = when (val replacement = rule.replaceWith) {
            null -> stringResource(MR.strings.book_reader_text_rule_hide)
            else -> stringResource(MR.strings.book_text_rule_summary_replace, replacement)
        },
        subtitleMaxLines = 1,
        trailing = {
            Row {
                IconButton(
                    icon = MaterialSymbols.Rounded.Edit,
                    contentDescription = stringResource(MR.strings.action_edit),
                    onClick = onEdit,
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                    onClick = onDelete,
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = onEnabledChange)
        },
        onClick = onEdit,
    )
}
