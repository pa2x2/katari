package mihon.presentation.kit.shortcut

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** [shortcuts] under their group headings, in the order given within each group. */
@Composable
internal fun KeyboardShortcutsSheet(shortcuts: List<KeyboardShortcut>, onDismissRequest: () -> Unit) {
    Sheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.keyboard_shortcuts))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = KatariTheme.spacing.l),
            ) {
                ShortcutGroup.entries.forEach { group ->
                    val grouped = shortcuts.filter { it.group == group }
                    if (grouped.isNotEmpty()) {
                        RowGroupHeader(group.title())
                        RowGroup(
                            rows = grouped.map { shortcut ->
                                {
                                    SettingsRow(
                                        title = shortcut.label,
                                        trailing = { ShortcutKeyCaps(shortcut.keys) },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutGroup.title(): String = when (this) {
    ShortcutGroup.Navigation -> stringResource(MR.strings.keyboard_shortcuts_navigation)
    ShortcutGroup.CurrentScreen -> stringResource(MR.strings.keyboard_shortcuts_current_screen)
    ShortcutGroup.Window -> stringResource(MR.strings.keyboard_shortcuts_window)
}
