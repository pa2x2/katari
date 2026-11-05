package mihon.presentation.kit.refresh

import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ScreenShortcuts
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A screen's refresh where it has no app bar, such as in a sheet header or over media. It owns F5 while it is shown;
 * [label] names what the refresh does when that is more than reloading, such as checking for updates.
 */
@Composable
fun RefreshButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    label: String = stringResource(MR.strings.action_refresh),
) {
    val shortcut = ScreenShortcuts.Refresh
    KeyboardShortcutHandler(shortcut, enabled = enabled, onTrigger = onClick)
    IconButton(
        icon = MaterialSymbols.Rounded.Refresh,
        contentDescription = label,
        onClick = onClick,
        enabled = enabled,
        shortcut = shortcut,
    )
}

/** A screen's refresh as an app bar icon, which owns F5 while the bar shows it. [title] works as in [RefreshButton]. */
@Composable
fun refreshAppBarAction(
    onClick: () -> Unit,
    enabled: Boolean = true,
    title: String = stringResource(MR.strings.action_refresh),
): AppBar.Action = AppBar.Action(
    title = title,
    icon = MaterialSymbols.Rounded.Refresh,
    onClick = onClick,
    enabled = enabled,
    shortcut = ScreenShortcuts.Refresh,
)
