package eu.kanade.presentation.more.settings.screen.textrules

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import mihon.entry.interactions.reader.settings.BookTextRule
import mihon.entry.interactions.reader.settings.BookTextRuleScope
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Edit
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.control.Switch
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One text rule: what it finds, what it does and where, with a switch that turns it off without deleting it. */
@Composable
internal fun BookTextRuleItem(
    rule: BookTextRule,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val scope = when (val scope = rule.scope) {
        is BookTextRuleScope.Series -> scope.title
        is BookTextRuleScope.Source -> scope.name
    }
    SettingsRow(
        title = if (rule.regex) "/${rule.find}/" else rule.find,
        subtitle = when (val replacement = rule.replaceWith) {
            null -> stringResource(MR.strings.book_text_rule_summary_hide, scope)
            else -> stringResource(MR.strings.book_text_rule_summary_replace, replacement, scope)
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
